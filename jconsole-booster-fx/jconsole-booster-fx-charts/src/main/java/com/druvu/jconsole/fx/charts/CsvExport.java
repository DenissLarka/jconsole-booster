package com.druvu.jconsole.fx.charts;

import com.druvu.jconsole.core.text.Formatter;
import java.io.IOException;
import java.util.Locale;

/**
 * Writes a chart's samples as CSV: a {@code Time,<series names>} header, then one row per sample with the time as an
 * Excel serial date ({@code %.6f}) and the value of each series. A gap leaves its value cells empty.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
final class CsvExport {

    private static final String LINE_END = System.lineSeparator();

    private CsvExport() {}

    static void write(SampleStore store, int decimals, Appendable out) throws IOException {
        // a decimal comma would split a value into two columns, so numbers take a point whatever the locale
        ValueFormat format = new ValueFormat(ChartUnit.NONE, decimals, Locale.ROOT);
        out.append("Time");
        for (Series series : store.series()) {
            out.append(',').append(cell(series.name()));
        }
        out.append(LINE_END);
        for (int i = 0; i < store.size(); i++) {
            out.append(String.format(Locale.ROOT, "%.6f", Formatter.toExcelTime(store.time(i))));
            for (Series series : store.series()) {
                out.append(',');
                long value = series.value(i);
                if (!SampleStore.isGap(value)) {
                    out.append(format.number(value, false));
                }
            }
            out.append(LINE_END);
        }
    }

    /** A header cell: quoted when the name holds a comma, a quote or a line break. */
    static String cell(String text) {
        if (text.contains(",") || text.contains("\"") || text.contains("\n") || text.contains("\r")) {
            return '"' + text.replace("\"", "\"\"") + '"';
        }
        return text;
    }
}
