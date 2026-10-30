package com.druvu.jconsole.fx.charts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Locale;
import org.testng.annotations.Test;

/** Checks how values read as numbers, percentages, byte sizes and axis labels. */
public class ValueFormatTest {

    @Test
    public void wholeNumbersGroupDigitsOnRequest() {
        ValueFormat format = new ValueFormat(ChartUnit.NONE, 0, Locale.US);

        assertThat(format.number(1_234_567, true)).isEqualTo("1,234,567");
        assertThat(format.number(1_234_567, false)).isEqualTo("1234567");
    }

    @Test
    public void decimalsShiftTheStoredValue() {
        ValueFormat format = new ValueFormat(ChartUnit.NONE, 2, Locale.US);

        assertThat(format.number(12_345, false)).isEqualTo("123.45");
        assertThat(format.number(1_234_567, true)).isEqualTo("12,345.67");
    }

    @Test
    public void percentagesEndInAPercentSign() {
        ValueFormat format = new ValueFormat(ChartUnit.PERCENT, 1, Locale.US);

        assertThat(format.currentValue(455)).isEqualTo("45.5%");
    }

    @Test
    public void tickLabelsDropTheZerosEveryLabelEndsWith() {
        ValueFormat format = new ValueFormat(ChartUnit.NONE, 2, Locale.US);

        assertThat(format.tickLabels(List.of(0L, 50L, 100L, 150L), 150)).containsExactly("0.0", "0.5", "1.0", "1.5");
    }

    @Test
    public void tickLabelsDropTheDecimalPointWhenNothingFollowsIt() {
        ValueFormat format = new ValueFormat(ChartUnit.PERCENT, 1, Locale.US);

        assertThat(format.tickLabels(List.of(0L, 10L, 20L), 20)).containsExactly("0%", "1%", "2%");
    }

    @Test
    public void byteTicksReadInSizeUnits() {
        ValueFormat format = new ValueFormat(ChartUnit.BYTES, 0, Locale.US);

        assertThat(format.tickLabels(List.of(100_000_000L, 600_000_000L), 600_000_000L))
                .containsExactly("100 Mb", "600 Mb");
    }

    @Test
    public void plainTicksGroupDigits() {
        ValueFormat format = new ValueFormat(ChartUnit.NONE, 0, Locale.US);

        assertThat(format.tickLabels(List.of(0L, 5_000L, 10_000L), 10_000)).containsExactly("0", "5,000", "10,000");
    }

    @Test
    public void negativeDecimalsAreRejected() {
        assertThatThrownBy(() -> new ValueFormat(ChartUnit.NONE, -1, Locale.US))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
