package com.druvu.jconsole.fx.tab.overview;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.management.MemoryUsage;
import org.testng.annotations.Test;

/** The words under the Overview's charts, as plain text. */
public class InfoLinesTest {

    @Test
    public void theHeapLineHasUsedCommittedAndMaxInByteSizes() {
        MemoryUsage heap = new MemoryUsage(0, 12_300_000, 20_000_000, 40_000_000);

        assertThat(InfoLines.heap(heap)).matches("Used: 12[.,]3 Mb    Committed: 20 Mb    Max: 40 Mb");
    }

    @Test
    public void theThreadsLineHasLivePeakAndTotal() {
        assertThat(InfoLines.threads(new ThreadCounts(12, 15, 40))).isEqualTo("Live: 12    Peak: 15    Total: 40");
    }

    @Test
    public void theClassesLineCountsTheUnloadedOnes() {
        assertThat(InfoLines.classes(new ClassCounts(900, 950))).isEqualTo("Loaded: 900    Unloaded: 50    Total: 950");
    }

    @Test
    public void theCpuLineHasOneDecimal() {
        assertThat(InfoLines.cpu(12.34)).matches("CPU Usage: 12[.,]3%");
    }
}
