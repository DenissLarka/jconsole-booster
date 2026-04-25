package com.druvu.jconsole.inspector.operations;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

/**
 * Markup hint embedded in a JMX description string. The first occurrence of
 * {@code {{tag}}} or {@code {{tag:options}}} in the description selects a
 * richer widget; the rest of the description (with the markup excised) is the
 * human-readable prose used as the tooltip.
 *
 * <p>Recognized tags are described in the Phase&nbsp;2.A1 design doc. Examples:
 * <pre>
 *   "Currency pair {{combo:EURUSD,USDCHF}}"      → tag=combo, options="EURUSD,USDCHF"
 *   "Settle date {{date:dd.MM.yyyy}}"            → tag=date,  options="dd.MM.yyyy"
 *   "Payload {{text:rows=10}}"                   → tag=text,  options="rows=10"
 *   "Upload {{file:*.csv}}"                      → tag=file,  options="*.csv"
 *   "Server config {{returns:format=json}}"      → tag=returns, options="format=json"
 *   "Plain description"                          → empty
 * </pre>
 */
@Slf4j
public record ParameterHint(String tag, String options, String prose) {

    private static final Pattern TAG = Pattern.compile("\\{\\{(\\w+)(?::([^}]*))?\\}\\}");

    public static Optional<ParameterHint> parse(String description) {
        if (description == null || description.isEmpty()) {
            return Optional.empty();
        }
        Matcher m = TAG.matcher(description);
        if (!m.find()) {
            return Optional.empty();
        }
        String tag = m.group(1).toLowerCase();
        String options = m.group(2);
        if (options == null) {
            options = "";
        }
        // Cleaned prose: strip the markup and squash any double spaces it left behind.
        String prose = (description.substring(0, m.start()) + description.substring(m.end()))
                .replaceAll("\\s{2,}", " ")
                .strip();
        if (m.find()) {
            log.warn(
                    "Multiple markup tags in description — only the first is honored: {}",
                    description);
        }
        return Optional.of(new ParameterHint(tag, options, prose));
    }

    /**
     * Parses the options string as comma-separated {@code key=value} pairs.
     * Bare tokens (no {@code =}) are stored with an empty value. Insertion
     * order is preserved.
     */
    public Map<String, String> optionsAsKeyValue() {
        Map<String, String> result = new LinkedHashMap<>();
        if (options.isEmpty()) {
            return result;
        }
        for (String pair : options.split(",")) {
            String trimmed = pair.strip();
            if (trimmed.isEmpty()) {
                continue;
            }
            int eq = trimmed.indexOf('=');
            if (eq < 0) {
                result.put(trimmed, "");
            } else {
                result.put(trimmed.substring(0, eq).strip(), trimmed.substring(eq + 1).strip());
            }
        }
        return result;
    }

    /** Parses the options as a comma-separated list of bare tokens (used by {@code combo} and {@code file}). */
    public java.util.List<String> optionsAsList() {
        if (options.isEmpty()) {
            return java.util.List.of();
        }
        java.util.List<String> result = new java.util.ArrayList<>();
        for (String token : options.split(",")) {
            String trimmed = token.strip();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
    }
}
