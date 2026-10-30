package com.druvu.jconsole.fx.tab.mbeans.viewers;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Comma-separated values as spreadsheets read them: CRLF line ends, a cell quoted when it holds a comma, a quote or a
 * line break.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
public final class Csv {

    private Csv() {}

    public static String of(List<String> header, List<List<String>> rows) {
        StringBuilder csv = new StringBuilder();
        appendLine(csv, header);
        rows.forEach(row -> appendLine(csv, row));
        return csv.toString();
    }

    static String escape(String cell) {
        boolean quoted = cell.contains(",") || cell.contains("\"") || cell.contains("\n") || cell.contains("\r");
        return quoted ? "\"" + cell.replace("\"", "\"\"") + "\"" : cell;
    }

    private static void appendLine(StringBuilder csv, List<String> cells) {
        csv.append(cells.stream().map(Csv::escape).collect(Collectors.joining(",")))
                .append("\r\n");
    }
}
