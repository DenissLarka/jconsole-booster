package com.druvu.jconsole.fx.tab.memory;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.testng.annotations.Test;

/** An area's usage against its maximum and threshold, and the areas' names and order. */
public class AreaUsageTest {

    private static final MemoryArea OLD_GEN = MemoryArea.pool("Old Gen", true);

    @Test
    public void aPoolIsAboveItsThresholdOnlyWhenItUsesMore() {
        assertThat(usage(80, 100, 50).isAboveThreshold()).isTrue();
        assertThat(usage(50, 100, 50).isAboveThreshold()).isFalse();
        assertThat(usage(20, 100, 50).isAboveThreshold()).isFalse();
    }

    @Test
    public void aThresholdOfZeroIsNone() {
        AreaUsage withoutThreshold = usage(80, 100, AreaUsage.NO_THRESHOLD);

        assertThat(withoutThreshold.hasThreshold()).isFalse();
        assertThat(withoutThreshold.isAboveThreshold()).isFalse();
        assertThat(usage(80, 100, 50).hasThreshold()).isTrue();
    }

    @Test
    public void measuresAgainstTheMaximum() {
        AreaUsage usage = usage(45, 200, 150);

        assertThat(usage.percentOfMax(usage.used())).isEqualTo(22);
        assertThat(usage.shareOfMax(usage.threshold())).isEqualTo(0.75);
        assertThat(usage.shareOfMax(300)).isEqualTo(1.0);
    }

    @Test
    public void aMaximumOfMinusOneIsNoLimit() {
        assertThat(usage(45, -1, 0).hasMax()).isFalse();
        assertThat(usage(45, 200, 0).hasMax()).isTrue();
    }

    @Test
    public void namesAPoolAsTheSwingJConsoleDoes() {
        assertThat(OLD_GEN.name()).isEqualTo("Memory Pool \"Old Gen\"");
        assertThat(OLD_GEN.toString()).isEqualTo(OLD_GEN.name());
        assertThat(OLD_GEN.isPool()).isTrue();
        assertThat(MemoryArea.HEAP.isPool()).isFalse();
    }

    @Test
    public void ordersHeapAndNonHeapFirstThenThePoolsByGroupAndName() {
        MemoryArea metaspace = MemoryArea.pool("Metaspace", false);
        MemoryArea codeHeap = MemoryArea.pool("CodeHeap", false);
        MemoryArea eden = MemoryArea.pool("Eden Space", true);
        List<MemoryArea> areas =
                new ArrayList<>(List.of(metaspace, OLD_GEN, MemoryArea.NON_HEAP, codeHeap, eden, MemoryArea.HEAP));

        areas.sort(MemoryArea.ORDER);

        assertThat(areas).containsExactly(MemoryArea.HEAP, MemoryArea.NON_HEAP, eden, OLD_GEN, codeHeap, metaspace);
    }

    private static AreaUsage usage(long used, long max, long threshold) {
        return new AreaUsage(OLD_GEN, used, used, max, threshold);
    }
}
