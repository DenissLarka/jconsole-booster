package com.druvu.jconsole.fx.tab.mbeans.info;

/**
 * One line of an info table: a name and its value, or a header that starts a block ({@code Attribute:},
 * {@code Parameter-0:}).
 *
 * @param name the property, or the header's text
 * @param value the property's value, empty for a header
 * @param header whether the row starts a block
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
public record InfoRow(String name, String value, boolean header) {

    static InfoRow header(String text) {
        return new InfoRow(text, "", true);
    }

    static InfoRow of(String name, Object value) {
        return new InfoRow(name, value == null ? "" : value.toString(), false);
    }
}
