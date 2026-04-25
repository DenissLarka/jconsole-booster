package com.druvu.jconsole.ui.menu;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

/**
 * Translates bookmark display names that use a tiny inline-markup vocabulary
 * ({@code *bold*}, {@code [color text]}) into {@code <html>...</html>}
 * snippets that {@link javax.swing.JMenuItem} renders. Names without any
 * markup are returned unchanged so JMenuItem keeps its default plain-text
 * rendering (cheaper, no HTML parsing).
 */
@Slf4j
public final class MarkupRenderer {

    private static final Pattern BOLD = Pattern.compile("\\*([^*]+)\\*");
    private static final Pattern COLOR = Pattern.compile("\\[(\\w+)\\s+(.+?)\\]");

    private static final Map<String, String> COLORS = Map.of(
            "red", "#c83232",
            "blue", "#3264c8",
            "green", "#32a05a",
            "orange", "#d28232",
            "gray", "#808080",
            "black", "#000000",
            "purple", "#8c46b4");

    private MarkupRenderer() {}

    public static String render(String displayName) {
        String boldApplied = BOLD.matcher(displayName).replaceAll("<b>$1</b>");
        Matcher m = COLOR.matcher(boldApplied);
        StringBuilder out = new StringBuilder();
        boolean changed = !boldApplied.equals(displayName);
        while (m.find()) {
            String colorName = m.group(1).toLowerCase();
            String text = m.group(2);
            String hex = COLORS.get(colorName);
            if (hex == null) {
                log.warn("Unknown bookmark color '{}' — leaving text unformatted", colorName);
                m.appendReplacement(out, Matcher.quoteReplacement(m.group(0)));
            } else {
                m.appendReplacement(out, Matcher.quoteReplacement("<font color=\"" + hex + "\">" + text + "</font>"));
                changed = true;
            }
        }
        m.appendTail(out);
        return changed ? "<html>" + out + "</html>" : displayName;
    }
}
