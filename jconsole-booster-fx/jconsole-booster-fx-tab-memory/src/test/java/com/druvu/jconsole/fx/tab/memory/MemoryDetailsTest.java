package com.druvu.jconsole.fx.tab.memory;

import static org.assertj.core.api.Assertions.assertThat;

import com.druvu.jconsole.fx.tab.DetailRow;
import java.util.List;
import org.testng.annotations.Test;

/** The details of the chosen area in words: which lines show, and how sizes and collector times read. */
public class MemoryDetailsTest {

    private static final long KBYTE = 1024;
    private static final MemoryArea OLD_GEN = MemoryArea.pool("Old Gen", true);
    private static final List<CollectorTime> COLLECTORS =
            List.of(new CollectorTime("Young", 3, 250), new CollectorTime("Old", 1, 40));

    @Test
    public void listsEveryValueOfAPoolThenTheCollectors() {
        AreaUsage usage = new AreaUsage(OLD_GEN, 512 * KBYTE, 768 * KBYTE, 900 * KBYTE, 600 * KBYTE);
        MemoryReading reading = new MemoryReading(System.currentTimeMillis(), List.of(usage), COLLECTORS);

        List<DetailRow> rows = MemoryDetails.rows(reading, OLD_GEN);

        assertThat(rows)
                .extracting(DetailRow::name)
                .containsExactly("Time", "Used", "Committed", "Max", "Usage Threshold", "GC time", "");
        assertThat(rows.subList(1, 5))
                .extracting(DetailRow::value)
                .containsExactly("512 kbytes", "768 kbytes", "900 kbytes", "600 kbytes");
        assertThat(rows.getFirst().value()).matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}");
    }

    @Test
    public void leavesOutTheValuesAnAreaDoesNotHave() {
        AreaUsage heap = new AreaUsage(MemoryArea.HEAP, 512 * KBYTE, 0, -1, AreaUsage.NO_THRESHOLD);
        MemoryReading reading = new MemoryReading(System.currentTimeMillis(), List.of(heap), List.of());

        assertThat(MemoryDetails.rows(reading, MemoryArea.HEAP))
                .extracting(DetailRow::name)
                .containsExactly("Time", "Used");
    }

    @Test
    public void anAreaMissingFromTheReadingShowsOnlyTheTimeAndTheCollectors() {
        MemoryReading reading = new MemoryReading(System.currentTimeMillis(), List.of(), COLLECTORS);

        assertThat(MemoryDetails.rows(reading, OLD_GEN))
                .extracting(DetailRow::name)
                .containsExactly("Time", "GC time", "");
    }

    @Test
    public void aCollectorReadsItsTimeNameAndCount() {
        assertThat(MemoryDetails.collectorText(COLLECTORS.getFirst()))
                .matches("0[.,]250 seconds on Young \\(3 collections\\)");
    }

    @Test
    public void aSizeReadsInWholeKilobytes() {
        assertThat(MemoryDetails.kbytes(KBYTE * 3 / 2)).isEqualTo("1 kbytes");
        assertThat(MemoryDetails.kbytes(0)).isEqualTo("0 kbytes");
    }
}
