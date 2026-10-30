package com.druvu.jconsole.fx.charts;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Arrays;
import org.testng.annotations.Test;

/** Checks the time ranges' spans, labels and order. */
public class TimeRangeTest {

    @Test
    public void fixedRangesSpanTheirLength() {
        assertThat(TimeRange.ONE_MINUTE.span()).contains(Duration.ofMinutes(1));
        assertThat(TimeRange.TWELVE_HOURS.span()).contains(Duration.ofHours(12));
        assertThat(TimeRange.SEVEN_DAYS.span()).contains(Duration.ofDays(7));
        assertThat(TimeRange.ONE_MONTH.span()).contains(Duration.ofDays(31));
        assertThat(TimeRange.ONE_YEAR.span()).contains(Duration.ofDays(366));
    }

    @Test
    public void allHasNoSpanAndIsTheDefault() {
        assertThat(TimeRange.ALL.span()).isEmpty();
        assertThat(TimeRange.DEFAULT).isEqualTo(TimeRange.ALL);
    }

    @Test
    public void labelsAreTheSharedMessagesWithoutPadding() {
        assertThat(TimeRange.ONE_MINUTE.label()).isEqualTo("1 min");
        assertThat(TimeRange.TWELVE_HOURS).hasToString("12 hours");
        assertThat(TimeRange.ALL.label()).isEqualTo("All");
    }

    @Test
    public void rangesRunFromTheShortestToAll() {
        assertThat(TimeRange.values())
                .hasSize(16)
                .startsWith(TimeRange.ONE_MINUTE)
                .endsWith(TimeRange.ALL);
        long[] spans = Arrays.stream(TimeRange.values())
                .filter(range -> range != TimeRange.ALL)
                .mapToLong(range -> range.span().orElseThrow().toMillis())
                .toArray();
        assertThat(spans).isSorted().doesNotHaveDuplicates();
    }
}
