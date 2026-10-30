package com.druvu.jconsole.fx.tab.overview;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.OptionalDouble;
import java.util.concurrent.TimeUnit;
import org.testng.annotations.Test;

/**
 * The CPU usage arithmetic: CPU time over elapsed time times processors, between 0 and 99%, from the second reading.
 */
public class CpuUsageTest {

    private static final long HALF_A_SECOND_IN_NANOS = TimeUnit.MILLISECONDS.toNanos(500);

    @Test
    public void theFirstReadingHasNoUsage() {
        assertThat(new CpuUsage().next(HALF_A_SECOND_IN_NANOS, 1000, 2)).isEmpty();
    }

    @Test
    public void usageIsTheCpuTimeOverTheElapsedTimeOfEveryProcessor() {
        CpuUsage usage = new CpuUsage();
        usage.next(0, 1000, 2);

        // half a second of CPU in one second of two processors
        OptionalDouble percent = usage.next(HALF_A_SECOND_IN_NANOS, 2000, 2);

        assertThat(percent.orElseThrow()).isCloseTo(25.0, within(1e-9));
    }

    @Test
    public void eachReadingCountsFromTheOneBefore() {
        CpuUsage usage = new CpuUsage();
        usage.next(0, 1000, 1);
        usage.next(HALF_A_SECOND_IN_NANOS, 2000, 1);

        OptionalDouble percent = usage.next(2 * HALF_A_SECOND_IN_NANOS, 4000, 1);

        assertThat(percent.orElseThrow()).isCloseTo(25.0, within(1e-9));
    }

    @Test
    public void anOvershootIsCappedBelowAHundred() {
        assertThat(CpuUsage.percent(TimeUnit.SECONDS.toNanos(2), 1000, 1)).isEqualTo(CpuUsage.MAX_PERCENT);
    }

    @Test
    public void aCpuTimeThatWentBackIsZero() {
        assertThat(CpuUsage.percent(-HALF_A_SECOND_IN_NANOS, 1000, 1)).isZero();
    }

    @Test
    public void anUnmeasuredCpuTimeHasNoUsageAndLeavesThePreviousReading() {
        CpuUsage usage = new CpuUsage();
        usage.next(0, 1000, 2);

        assertThat(usage.next(-1, 1500, 2)).isEmpty();
        assertThat(usage.next(HALF_A_SECOND_IN_NANOS, 2000, 2).orElseThrow()).isCloseTo(25.0, within(1e-9));
    }

    @Test
    public void noUsageWhenNoTimeHasPassed() {
        CpuUsage usage = new CpuUsage();
        usage.next(0, 1000, 1);

        assertThat(usage.next(HALF_A_SECOND_IN_NANOS, 1000, 1)).isEmpty();
    }
}
