package com.druvu.jconsole.fx.tab.summary;

import java.util.Objects;

/**
 * One line of the summary page: what is shown and its value, already formatted.
 *
 * @param label what the value is, e.g. {@code Uptime}
 * @param value the value as text
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
record SummaryRow(String label, String value) {

    SummaryRow {
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(value, "value");
    }
}
