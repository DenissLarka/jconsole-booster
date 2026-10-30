package com.druvu.jconsole.fx.tab.summary;

import java.util.List;
import java.util.Objects;

/**
 * A titled group of rows on the summary page, such as Threads or Memory.
 *
 * @param title the group's heading
 * @param rows the group's lines, in display order
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
record SummarySection(String title, List<SummaryRow> rows) {

    SummarySection {
        Objects.requireNonNull(title, "title");
        rows = List.copyOf(rows);
    }
}
