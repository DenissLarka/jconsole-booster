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
 * Handles {@code byte[]} return values that an operation declares with a
 * {@code {{returns:mime=…}}} hint. On a whitelisted MIME type the bytes are
 * written to a temp file and opened with the OS default handler. Anything
 * else falls through to a confirmation + Save-As dialog so the user has the
 * final say before launching unknown content.
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
            "image/svg+xml");

    private static final Map<String, String> EXTENSIONS = Map.ofEntries(
            Map.entry("application/pdf", ".pdf"),
            Map.entry("application/json", ".json"),
            Map.entry("application/xml", ".xml"),
            Map.entry("application/zip", ".zip"),
            Map.entry("text/plain", ".txt"),
            Map.entry("text/csv", ".csv"),
            Map.entry("text/html", ".html"),
            Map.entry("image/png", ".png"),
            Map.entry("image/jpeg", ".jpg"),
            Map.entry("image/gif", ".gif"),
            Map.entry("image/svg+xml", ".svg"));

    private static final String UNKNOWN_EXTENSION = ".bin";

    private MimeHandler() {}

    /** Pulls the MIME value out of a {@code {{returns:mime=…}}} markup tag, if any. */
    public static Optional<String> mimeFromDescription(String operationDescription) {
        return ParameterHint.parse(operationDescription)
                .filter(h -> "returns".equals(h.tag()))
                .map(h -> h.optionsAsKeyValue().get("mime"))
                .filter(s -> s != null && !s.isBlank())
                .map(String::toLowerCase);
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
     * Performs the appropriate user-facing action for a byte[] return on the EDT.
     * Returns {@code true} if this method handled the result (the caller should
     * NOT fall through to the generic array viewer); {@code false} if no hint
     * was present.
     */
    public static boolean handle(Component parent, String operationName, String operationDescription, byte[] data) {
        Optional<String> mimeOpt = mimeFromDescription(operationDescription);
        if (mimeOpt.isEmpty()) {
            return false;
        }
        String mime = mimeOpt.get();
        if (isOnWhitelist(mime)) {
            openInDefaultApp(parent, operationName, mime, data);
        } else {
            promptThenSave(parent, operationName, mime, data);
        }
        return true;
    }

    private static void openInDefaultApp(Component parent, String operationName, String mime, byte[] data) {
        try {
            Path temp = Files.createTempFile("jcb-" + sanitize(operationName) + "-", extensionFor(mime));
            Files.write(temp, data);
            temp.toFile().deleteOnExit();
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(temp.toFile());
                return;
            }
            log.info("Desktop OPEN unsupported — falling back to Save-As for {}", temp);
            saveAs(parent, operationName, mime, data);
        } catch (IOException ex) {
            log.warn("Could not open byte[] result as {}: {}", mime, ex.getMessage());
            JOptionPane.showMessageDialog(
                    parent,
                    "Could not open returned file: " + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void promptThenSave(Component parent, String operationName, String mime, byte[] data) {
        int answer = JOptionPane.showConfirmDialog(
                parent,
                "This operation returned a file of type \""
                        + mime
                        + "\".\n"
                        + "Files of unknown types may be unsafe — open only if you trust the server.\n\n"
                        + "Save the file to disk?",
                "Untrusted file type",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (answer == JOptionPane.OK_OPTION) {
            saveAs(parent, operationName, mime, data);
        }
    }

    private static void saveAs(Component parent, String operationName, String mime, byte[] data) {
        JFileChooser chooser = new JFileChooser();
        chooser.setPreferredSize(new Dimension(900, 600));
        chooser.setSelectedFile(new java.io.File(sanitize(operationName) + extensionFor(mime)));
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

    private static String sanitize(String name) {
        return name.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
