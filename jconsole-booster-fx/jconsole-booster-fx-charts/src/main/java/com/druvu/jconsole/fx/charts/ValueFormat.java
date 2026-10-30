package com.druvu.jconsole.fx.charts;

import com.druvu.jconsole.core.text.Formatter;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Writes a chart's values as text: fixed-point decimals, byte sizes and percentages, as the axis, the current values
 * and the CSV export show them.
 *
 * <p>A value with decimals is stored shifted left by that many decimal places: 1.5 with one decimal is stored as 15.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
final class ValueFormat {

    private final ChartUnit unit;
    private final int decimals;
    private final Locale locale;

    ValueFormat(ChartUnit unit, int decimals, Locale locale) {
        if (decimals < 0) {
            throw new IllegalArgumentException("decimals must not be negative: " + decimals);
        }
        this.unit = Objects.requireNonNull(unit, "unit");
        this.decimals = decimals;
        this.locale = Objects.requireNonNull(locale, "locale");
    }

    ChartUnit unit() {
        return unit;
    }

    int decimals() {
        return decimals;
    }

    /** {@code value} as a number with the chart's decimals, its digits grouped or not. */
    String number(long value, boolean groupDigits) {
        if (decimals == 0) {
            return String.format(locale, groupDigits ? "%,d" : "%d", value);
        }
        String pattern = (groupDigits ? "%,." : "%.") + decimals + "f";
        return String.format(locale, pattern, value / Math.pow(10, decimals));
    }

    /** The label of a line's newest value: digits grouped, and a {@code %} for a percentage. */
    String currentValue(long value) {
        return number(value, true) + percentSign();
    }

    /**
     * The labels of the value axis. Byte sizes read in Kb, Mb or Gb by the size of the axis top; with decimals, the
     * zeros every label ends with are dropped, so 0.50 and 1.00 read 0.5 and 1.0.
     */
    List<String> tickLabels(List<Long> ticks, long max) {
        List<String> labels = new ArrayList<>();
        for (long tick : ticks) {
            labels.add(
                    unit == ChartUnit.BYTES && decimals == 0 ? Formatter.formatBytes(tick, max) : number(tick, true));
        }
        if (decimals > 0) {
            labels = withoutCommonTrailingZeros(labels);
        }
        return labels.stream().map(label -> label + percentSign()).toList();
    }

    private List<String> withoutCommonTrailingZeros(List<String> labels) {
        List<String> trimmed = labels;
        while (allEndWith(trimmed, '0')) {
            trimmed = trimmed.stream().map(ValueFormat::withoutLastCharacter).toList();
        }
        if (allEndWith(trimmed, DecimalFormatSymbols.getInstance(locale).getDecimalSeparator())) {
            trimmed = trimmed.stream().map(ValueFormat::withoutLastCharacter).toList();
        }
        return trimmed;
    }

    private static boolean allEndWith(List<String> labels, char last) {
        return !labels.isEmpty()
                && labels.stream().allMatch(label -> !label.isEmpty() && label.charAt(label.length() - 1) == last);
    }

    private static String withoutLastCharacter(String text) {
        return text.substring(0, text.length() - 1);
    }

    private String percentSign() {
        return unit == ChartUnit.PERCENT ? "%" : "";
    }
}
