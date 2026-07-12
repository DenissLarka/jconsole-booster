package com.druvu.jconsole.inspector.operations;

import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import lombok.extern.slf4j.Slf4j;

/**
 * Handles {@code byte[]} return values that an operation declares with a {@code {{returns:mime=…}}} hint. The user is
 * always shown a Cancel / Download / Open choice — nothing is written or launched without an explicit click. For a
 * whitelisted MIME type all three options are offered; for anything else the OS-default {@code Open} is withheld
 * (Download only) so unknown content is never handed to the platform handler, even on request.
 *
 * <p>The hint may carry an optional {@code filename} sub-key — the JMX analog of an HTTP {@code Content-Disposition:
 * attachment; filename=} — e.g. {@code {{returns:mime=text/csv,filename=trades.csv}}}. It is server-supplied and
 * therefore untrusted: it is reduced to a basename and sanitized before it ever touches the filesystem (no path
 * traversal, no absolute paths).
 */
@Slf4j
public final class MimeHandler {

    private static final Set<String> WHITELIST = Set.of(
            "application/pdf",
            "application/json",
            "application/xml",
            "text/plain",
            "text/csv",
            "text/html",
            "image/png",
            "image/jpeg",
            "image/gif",
            "image/svg+xml",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "application/vnd.ms-excel");

    private static final Map<String, String> EXTENSIONS = Map.ofEntries(
            Map.entry("application/pdf", ".pdf"),
            Map.entry("application/json", ".json"),
            Map.entry("application/xml", ".xml"),
            Map.entry("application/zip", ".zip"),
            Map.entry("application/gzip", ".gz"),
            Map.entry("text/plain", ".txt"),
            Map.entry("text/csv", ".csv"),
            Map.entry("text/html", ".html"),
            Map.entry("image/png", ".png"),
            Map.entry("image/jpeg", ".jpg"),
            Map.entry("image/gif", ".gif"),
            Map.entry("image/svg+xml", ".svg"),
            Map.entry("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", ".xlsx"),
            Map.entry("application/vnd.openxmlformats-officedocument.wordprocessingml.document", ".docx"),
            Map.entry("application/vnd.openxmlformats-officedocument.presentationml.presentation", ".pptx"),
            Map.entry("application/vnd.ms-excel", ".xls"));

    private static final String UNKNOWN_EXTENSION = ".bin";

    private MimeHandler() {}

    /** Pulls the MIME value out of a {@code {{returns:mime=…}}} markup tag, if any. */
    public static Optional<String> mimeFromDescription(String operationDescription) {
        return returnsOption(operationDescription, "mime").map(String::toLowerCase);
    }

    /**
     * Pulls the optional server-supplied {@code filename} out of a {@code {{returns:…,filename=…}}} markup tag. The raw
     * value is untrusted; callers must run it through {@link #resolveFilename} (which sanitizes) before use.
     */
    public static Optional<String> filenameFromDescription(String operationDescription) {
        return returnsOption(operationDescription, "filename");
    }

    private static Optional<String> returnsOption(String operationDescription, String key) {
        return ParameterHint.parse(operationDescription)
                .filter(h -> "returns".equals(h.tag()))
                .map(h -> h.optionsAsKeyValue().get(key))
                .filter(s -> s != null && !s.isBlank());
    }

    public static boolean isOnWhitelist(String mime) {
        return mime != null && WHITELIST.contains(mime.toLowerCase());
    }

    /** Returns the extension (with leading dot) for a known MIME type, or {@code .bin} otherwise. */
    public static String extensionFor(String mime) {
        if (mime == null) {
            return UNKNOWN_EXTENSION;
        }
        return EXTENSIONS.getOrDefault(mime.toLowerCase(), UNKNOWN_EXTENSION);
    }

    /**
     * Chooses the download filename. A server-supplied name (untrusted) contributes the base name ONLY — it is reduced
     * to a basename, sanitized, and stripped of its extension; the extension always comes from the MIME type, never
     * from the server (otherwise a whitelisted MIME plus {@code filename=evil.exe} would let the Open path launch an
     * executable). Falls back to the operation name when no usable server name is present.
     */
    static String resolveFilename(String operationName, String mime, String serverFilename) {
        String base = "";
        if (serverFilename != null && !serverFilename.isBlank()) {
            base = stripExtension(sanitize(basename(serverFilename)));
        }
        if (base.isEmpty() || base.equals("_")) {
            base = sanitize(operationName);
        }
        return base + extensionFor(mime);
    }

    /**
     * Performs the appropriate user-facing action for a byte[] return on the EDT. Returns {@code true} if this method
     * handled the result (the caller should NOT fall through to the generic array viewer); {@code false} if no hint was
     * present.
     */
    public static boolean handle(Component parent, String operationName, String operationDescription, byte[] data) {
        Optional<String> mimeOpt = mimeFromDescription(operationDescription);
        if (mimeOpt.isEmpty()) {
            return false;
        }
        String mime = mimeOpt.get();
        String filename = resolveFilename(
                operationName,
                mime,
                filenameFromDescription(operationDescription).orElse(null));
        boolean canOpen = isOnWhitelist(mime);

        Object[] options = canOpen ? new Object[] {"Open", "Download", "Cancel"} : new Object[] {"Download", "Cancel"};
        String message = "This operation returned a file:\n\n"
                + "    " + filename + "\n"
                + "    type: " + mime + "\n\n"
                + (canOpen
                        ? "Open it with the default application, or download it to disk."
                        : "This is an unknown/unsafe type — it will not be opened automatically.\n"
                                + "Download it only if you trust the server.");
        int choice = JOptionPane.showOptionDialog(
                parent,
                message,
                "Returned file",
                JOptionPane.DEFAULT_OPTION,
                canOpen ? JOptionPane.QUESTION_MESSAGE : JOptionPane.WARNING_MESSAGE,
                null,
                options,
                // Default to Cancel (last option) so a stray Enter never launches/saves server-controlled content.
                options[options.length - 1]);

        String selected = (choice >= 0 && choice < options.length) ? (String) options[choice] : "Cancel";
        switch (selected) {
            case "Open" -> openInDefaultApp(parent, filename, mime, data);
            case "Download" -> saveAs(parent, filename, mime, data);
            default -> {
                /* Cancel / dialog closed — nothing to do. */
            }
        }
        return true;
    }

    private static void openInDefaultApp(Component parent, String filename, String mime, byte[] data) {
        try {
            // Write into a private temp DIRECTORY under the resolved name, so the OS handler sees a meaningful
            // filename (not a random temp suffix) and the extension drives the file-type association.
            Path dir = Files.createTempDirectory("jcb-open-");
            Path temp = dir.resolve(filename);
            Files.write(temp, data);
            // deleteOnExit runs in REVERSE registration order, so register the directory FIRST — then at exit the
            // file is deleted before the (now-empty) directory, otherwise the non-empty dir delete fails and leaks.
            dir.toFile().deleteOnExit();
            temp.toFile().deleteOnExit();
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(temp.toFile());
                return;
            }
            log.info("Desktop OPEN unsupported — falling back to Save-As for {}", temp);
            saveAs(parent, filename, mime, data);
        } catch (IOException ex) {
            log.warn("Could not open byte[] result as {}: {}", mime, ex.getMessage());
            JOptionPane.showMessageDialog(
                    parent, "Could not open returned file: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void saveAs(Component parent, String filename, String mime, byte[] data) {
        JFileChooser chooser = new JFileChooser();
        chooser.setPreferredSize(new Dimension(900, 600));
        chooser.setSelectedFile(new java.io.File(filename));
        if (chooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path target = chooser.getSelectedFile().toPath();
        try {
            Files.write(target, data);
            log.info("Wrote {} bytes ({}) to {}", data.length, mime, target);
        } catch (IOException ex) {
            log.warn("Could not write byte[] result to {}: {}", target, ex.getMessage());
            JOptionPane.showMessageDialog(
                    parent, "Could not save: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Strips any directory component from a server-supplied name (defends against path traversal / absolute paths). */
    private static String basename(String name) {
        String n = name.replace('\\', '/');
        int slash = n.lastIndexOf('/');
        return slash >= 0 ? n.substring(slash + 1) : n;
    }

    /** Drops a trailing {@code .ext}, but leaves dotfiles like {@code .bashrc} intact (leading dot, no base). */
    private static String stripExtension(String name) {
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    private static String sanitize(String name) {
        return name.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
