package com.druvu.jconsole.fx.charts;

import com.druvu.jconsole.core.text.Messages;
import java.time.Duration;
import java.util.Optional;

/**
 * How much time a chart shows: the newest stretch of a fixed length, or all of the data.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
public enum TimeRange {
    ONE_MINUTE(Messages.ONE_MIN, Duration.ofMinutes(1)),
    FIVE_MINUTES(Messages.FIVE_MIN, Duration.ofMinutes(5)),
    TEN_MINUTES(Messages.TEN_MIN, Duration.ofMinutes(10)),
    THIRTY_MINUTES(Messages.THIRTY_MIN, Duration.ofMinutes(30)),
    ONE_HOUR(Messages.ONE_HOUR, Duration.ofHours(1)),
    TWO_HOURS(Messages.TWO_HOURS, Duration.ofHours(2)),
    THREE_HOURS(Messages.THREE_HOURS, Duration.ofHours(3)),
    SIX_HOURS(Messages.SIX_HOURS, Duration.ofHours(6)),
    TWELVE_HOURS(Messages.TWELVE_HOURS, Duration.ofHours(12)),
    ONE_DAY(Messages.ONE_DAY, Duration.ofDays(1)),
    SEVEN_DAYS(Messages.SEVEN_DAYS, Duration.ofDays(7)),
    // a month counts 31 days and a year 366, so a range always holds a whole calendar month or year
    ONE_MONTH(Messages.ONE_MONTH, Duration.ofDays(31)),
    THREE_MONTHS(Messages.THREE_MONTHS, Duration.ofDays(3 * 31)),
    SIX_MONTHS(Messages.SIX_MONTHS, Duration.ofDays(6 * 31)),
    ONE_YEAR(Messages.ONE_YEAR, Duration.ofDays(366)),
    ALL(Messages.ALL, null);

    /** The range a new chart shows. */
    public static final TimeRange DEFAULT = ALL;

    private final String label;
    private final Duration span;

    TimeRange(String label, Duration span) {
        // the shared messages pad short names with a space to line them up in Swing; a JavaFX list does not need it
        this.label = label.strip();
        this.span = span;
    }

    /** The name shown in menus and in the chooser. */
    public String label() {
        return label;
    }

    /** How long the range is, or empty for {@link #ALL}. */
    public Optional<Duration> span() {
        return Optional.ofNullable(span);
    }

    @Override
    public String toString() {
        return label;
    }
}
