package com.druvu.jconsole.fx.tab.mbeans.info;

/**
 * One line of the info table: a name and its value, a header that starts a block ({@code Attribute:},
 * {@code Parameter-0:}), or the start of the descriptor section.
 *
 * @param name the property, or the header's or section's text
 * @param value the property's value, empty for a header or a section
 * @param kind what the row is, which sets how it looks
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
public record InfoRow(String name, String value, Kind kind) {

    /** What a row is. */
    public enum Kind {
        /** Starts the descriptor section, below every block of properties. */
        SECTION,
        /** Starts a block: the feature, or one of its parameters or constructors. */
        HEADER,
        /** A name and its value. */
        PROPERTY
    }

    static InfoRow section(String text) {
        return new InfoRow(text, "", Kind.SECTION);
    }

    static InfoRow header(String text) {
        return new InfoRow(text, "", Kind.HEADER);
    }

    static InfoRow of(String name, Object value) {
        return new InfoRow(name, value == null ? "" : value.toString(), Kind.PROPERTY);
    }
}
