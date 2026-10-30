package com.druvu.jconsole.fx.charts;

import static org.assertj.core.api.Assertions.assertThat;

import com.druvu.jconsole.core.text.Formatter;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import javafx.scene.paint.Color;
import org.testng.annotations.Test;

/** Checks the CSV a chart saves: its header, its rows, gaps and decimals. */
public class CsvExportTest {

    private static final long T0 = Instant.parse("2026-10-30T10:00:00Z").toEpochMilli();
    private static final long T1 = T0 + 4000;

    @Test
    public void writesAHeaderThenOneRowPerSample() throws IOException {
        SampleStore store = usedAndMax();
        store.add(T0, 100, 2000);
        store.add(T1, 150, 2000);

        assertThat(csv(store, 0))
                .containsExactly("Time,Used,\"Max, total\"", excelTime(T0) + ",100,2000", excelTime(T1) + ",150,2000");
    }

    @Test
    public void aGapLeavesItsCellsEmpty() throws IOException {
        SampleStore store = usedAndMax();
        store.add(T0, 100, 2000);
        store.addGap(T1);

        assertThat(csv(store, 0)).endsWith(excelTime(T1) + ",,");
    }

    @Test
    public void decimalsTakeAPointWhateverTheLocale() throws IOException {
        SampleStore store = usedAndMax();
        store.add(T0, 1_234_567, 5);
        Locale before = Locale.getDefault();
        Locale.setDefault(Locale.GERMANY);
        try {
            assertThat(csv(store, 2)).endsWith(excelTime(T0) + ",12345.67,0.05");
        } finally {
            Locale.setDefault(before);
        }
    }

    @Test
    public void anEmptyChartWritesTheHeaderOnly() throws IOException {
        assertThat(csv(usedAndMax(), 0)).containsExactly("Time,Used,\"Max, total\"");
    }

    @Test
    public void namesWithCommasOrQuotesAreQuoted() {
        assertThat(CsvExport.cell("Used")).isEqualTo("Used");
        assertThat(CsvExport.cell("Max, total")).isEqualTo("\"Max, total\"");
        assertThat(CsvExport.cell("the \"peak\"")).isEqualTo("\"the \"\"peak\"\"\"");
    }

    private static List<String> csv(SampleStore store, int decimals) throws IOException {
        StringBuilder out = new StringBuilder();
        CsvExport.write(store, decimals, out);
        return out.toString().lines().toList();
    }

    private static String excelTime(long time) {
        return String.format(Locale.ROOT, "%.6f", Formatter.toExcelTime(time));
    }

    private static SampleStore usedAndMax() {
        SampleStore store = new SampleStore();
        store.addSeries(new Series("used", "Used", Color.RED, true));
        store.addSeries(new Series("max", "Max, total", Color.BLUE, false));
        return store;
    }
}
