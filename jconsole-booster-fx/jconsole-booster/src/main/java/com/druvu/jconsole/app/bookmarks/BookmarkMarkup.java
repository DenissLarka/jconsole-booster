package com.druvu.jconsole.app.bookmarks;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A bookmark label's display markup, as 1.1 wrote it: {@code *bold*} and {@code [color text]}, nested either way
 * ({@code [red *prod*]} or {@code *[red prod]*}). An unknown colour is left as written, so a typo stays visible.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
final class BookmarkMarkup {

    /**
     * A stretch of a label with one look.
     *
     * @param color a CSS colour, or {@code null} for the theme's text colour
     */
    record Run(String text, boolean bold, String color) {

        boolean styled() {
            return bold || color != null;
        }
    }

    // [color text] or *text*: whichever comes first in the label
    private static final Pattern MARKUP = Pattern.compile("\\[(\\w+)\\s+(.+?)\\]|\\*([^*]+)\\*");

    // 1.1's palette
    private static final Map<String, String> COLORS = Map.of(
            "red", "#c83232",
            "blue", "#3264c8",
            "green", "#32a05a",
            "orange", "#d28232",
            "gray", "#808080",
            "purple", "#8c46b4");
    // black is the theme's own text colour: black in a light theme, still readable in a dark one
    private static final String BLACK = "black";

    private BookmarkMarkup() {}

    /** The label as plain text: the markup gone, its inner text kept. */
    static String plain(String label) {
        return runs(label).stream().map(Run::text).collect(Collectors.joining());
    }

    /** The label in stretches of one look each, in order; empty for no label. */
    static List<Run> runs(String label) {
        List<Run> runs = new ArrayList<>();
        if (label != null) {
            addRuns(label, false, null, runs);
        }
        return runs;
    }

    /** Adds the runs of {@code text}, which already has the look {@code bold} and {@code color} from outside. */
    private static void addRuns(String text, boolean bold, String color, List<Run> runs) {
        Matcher markup = MARKUP.matcher(text);
        int plainFrom = 0;
        while (markup.find()) {
            String colorName = markup.group(1);
            if (colorName != null && !isColor(colorName)) {
                // an unknown colour stays as written: it is plain text and the search goes on after it
                continue;
            }
            addText(text.substring(plainFrom, markup.start()), bold, color, runs);
            if (colorName != null) {
                addRuns(markup.group(2), bold, COLORS.get(colorName.toLowerCase(Locale.ROOT)), runs);
            } else {
                addRuns(markup.group(3), true, color, runs);
            }
            plainFrom = markup.end();
        }
        addText(text.substring(plainFrom), bold, color, runs);
    }

    private static void addText(String text, boolean bold, String color, List<Run> runs) {
        if (!text.isEmpty()) {
            runs.add(new Run(text, bold, color));
        }
    }

    private static boolean isColor(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        return COLORS.containsKey(lower) || BLACK.equals(lower);
    }
}
