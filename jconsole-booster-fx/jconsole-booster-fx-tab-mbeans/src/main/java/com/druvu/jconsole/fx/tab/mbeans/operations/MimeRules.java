package com.druvu.jconsole.fx.tab.mbeans.operations;

import com.druvu.jconsole.core.operations.ParameterHint;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The rules for a {@code byte[]} an operation returns as a file, declared by {@code {{returns:mime=…[,filename=…]}}} in
 * its description. Only whitelisted types may be opened with the OS application; anything may be downloaded. The file
 * name comes from the server, so it is untrusted: only its sanitised base name is kept, and the extension always comes
 * from the MIME type, so a whitelisted type cannot smuggle in an executable name.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
public final class MimeRules {

    private static final Set<String> OPENABLE = Set.of(
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

    private MimeRules() {}

    /** The MIME type the operation's description declares, lower case. */
    public static Optional<String> mimeOf(String operationDescription) {
        return returnsOption(operationDescription, "mime").map(mime -> mime.toLowerCase(Locale.ROOT));
    }

    /** The file name the server suggests, raw and untrusted: pass it through {@link #fileName} before use. */
    public static Optional<String> suggestedName(String operationDescription) {
        return returnsOption(operationDescription, "filename");
    }

    /** Whether the type may be opened with the OS application. */
    public static boolean isOpenable(String mime) {
        return mime != null && OPENABLE.contains(mime.toLowerCase(Locale.ROOT));
    }

    /** The extension for the type, with its dot; {@code .bin} for an unknown type. */
    public static String extensionFor(String mime) {
        return mime == null
                ? UNKNOWN_EXTENSION
                : EXTENSIONS.getOrDefault(mime.toLowerCase(Locale.ROOT), UNKNOWN_EXTENSION);
    }

    /**
     * The name to save or open the file under: the server's base name, sanitised and without its extension, or the
     * operation's name when the server gives none; then the type's extension.
     */
    public static String fileName(String operationName, String mime, String suggestedName) {
        String base = "";
        if (suggestedName != null && !suggestedName.isBlank()) {
            base = stripExtension(sanitize(baseName(suggestedName)));
        }
        if (base.isEmpty() || "_".equals(base)) {
            base = sanitize(operationName);
        }
        return base + extensionFor(mime);
    }

    private static Optional<String> returnsOption(String operationDescription, String key) {
        return ParameterHint.parse(operationDescription)
                .filter(hint -> "returns".equals(hint.tag()))
                .map(hint -> hint.optionsAsKeyValue().get(key))
                .filter(value -> !value.isBlank());
    }

    /** Drops any directory part, so the name cannot climb out of the folder or be absolute. */
    private static String baseName(String name) {
        String forward = name.replace('\\', '/');
        return forward.substring(forward.lastIndexOf('/') + 1);
    }

    /** Drops a trailing extension; a dot file such as {@code .bashrc} keeps its name. */
    private static String stripExtension(String name) {
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    private static String sanitize(String name) {
        return name.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
