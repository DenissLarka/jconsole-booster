package com.druvu.jconsole.fx.tab.summary;

import java.util.List;
import java.util.Objects;

/**
 * Everything the VM Summary tab shows, read from the target JVM in one pass.
 *
 * @param readAt when the values were read, formatted for the page's heading
 * @param sections the short values, in groups (virtual machine, threads, classes, memory, collectors, system)
 * @param paths the long values, one entry per line: the VM arguments and the class, library and boot class paths
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
record VmSummary(String readAt, List<SummarySection> sections, List<SummaryRow> paths) {

    VmSummary {
        Objects.requireNonNull(readAt, "readAt");
        sections = List.copyOf(sections);
        paths = List.copyOf(paths);
    }
}
