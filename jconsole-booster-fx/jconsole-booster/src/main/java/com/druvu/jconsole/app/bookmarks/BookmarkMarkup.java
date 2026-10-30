package com.druvu.jconsole.app.bookmarks;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads a bookmark's label without its display markup: {@code *bold*} and {@code [color text]} become their inner text.
 * Menus and lists show labels as plain text; an unknown colour is left as written, so a typo stays visible.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
final class BookmarkMarkup {

    private static final Pattern BOLD = Pattern.compile("\\*([^*]+)\\*");
    private static final Pattern COLOR = Pattern.compile("\\[(\\w+)\\s+(.+?)\\]");
    private static final Set<String> COLORS = Set.of("red", "blue", "green", "orange", "gray", "black", "purple");

    private BookmarkMarkup() {}

    /** The label as plain text. */
    static String plain(String label) {
        if (label == null) {
            return "";
        }
        Matcher color = COLOR.matcher(BOLD.matcher(label).replaceAll("$1"));
        StringBuilder plain = new StringBuilder();
        while (color.find()) {
            boolean known = COLORS.contains(color.group(1).toLowerCase(Locale.ROOT));
            color.appendReplacement(plain, Matcher.quoteReplacement(known ? color.group(2) : color.group(0)));
        }
        color.appendTail(plain);
        return plain.toString();
    }
}
