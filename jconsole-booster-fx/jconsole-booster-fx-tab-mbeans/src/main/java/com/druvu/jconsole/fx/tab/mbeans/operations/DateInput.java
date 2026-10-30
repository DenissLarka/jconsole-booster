package com.druvu.jconsole.fx.tab.mbeans.operations;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;

/**
 * The input of a {@code {{date:format}}} parameter: a date picker that reads and writes the format, today unless a
 * value is remembered. A format the picker cannot write (a time of day, a typo) falls back to a text field.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
final class DateInput {

    private DateInput() {}

    static ParameterInput create(String format, String preset) {
        Optional<DateTimeFormatter> formatter = formatter(format);
        if (formatter.isEmpty()) {
            TextField field = new TextField(preset);
            return new ControlInput(field, field, field::getText);
        }
        DatePicker picker = new DatePicker();
        picker.setConverter(new DateConverter(formatter.get()));
        picker.setValue(parse(formatter.get(), preset).orElse(LocalDate.now()));
        picker.setPromptText(format);
        // the text as typed: a value the format cannot read still reaches the MBean, which may know better
        return new ControlInput(
                picker, picker.getEditor(), () -> picker.getEditor().getText());
    }

    /** The formatter for a format that can write a date; empty otherwise. */
    static Optional<DateTimeFormatter> formatter(String format) {
        if (format == null || format.isBlank()) {
            return Optional.empty();
        }
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
            formatter.format(LocalDate.now());
            return Optional.of(formatter);
        } catch (IllegalArgumentException | DateTimeException e) {
            return Optional.empty();
        }
    }

    private static Optional<LocalDate> parse(DateTimeFormatter formatter, String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(LocalDate.parse(text.strip(), formatter));
        } catch (DateTimeException e) {
            return Optional.empty();
        }
    }

    /** Date to text and back in the parameter's format; text that does not parse gives no date. */
    private static final class DateConverter extends StringConverter<LocalDate> {

        private final DateTimeFormatter formatter;

        DateConverter(DateTimeFormatter formatter) {
            this.formatter = formatter;
        }

        @Override
        public String toString(LocalDate date) {
            return date == null ? "" : formatter.format(date);
        }

        @Override
        public LocalDate fromString(String text) {
            return parse(formatter, text).orElse(null);
        }
    }
}
