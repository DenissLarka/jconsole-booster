package com.druvu.jconsole.fx.tab.mbeans.operations;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import javax.management.MBeanParameterInfo;
import org.testng.annotations.Test;

/** The input a parameter gets from its description markup and its type. */
public class ParameterKindTest {

    @Test
    public void aComboListsItsValues() {
        assertThat(kind("Pair {{combo:EURUSD, USDCHF}}", "java.lang.String"))
                .isEqualTo(new ParameterKind.Choice(List.of("EURUSD", "USDCHF")));
    }

    @Test
    public void aComboWithoutValuesIsATextField() {
        assertThat(kind("Pair {{combo:}}", "java.lang.String")).isEqualTo(new ParameterKind.SingleLine());
    }

    @Test
    public void aDateKeepsItsFormat() {
        assertThat(kind("Settle {{date:dd.MM.yyyy}}", "java.lang.String"))
                .isEqualTo(new ParameterKind.DateText("dd.MM.yyyy"));
    }

    @Test
    public void aTextAreaTakesItsSizeOrTheDefaults() {
        assertThat(kind("Query {{text:rows=10,cols=40}}", "java.lang.String"))
                .isEqualTo(new ParameterKind.MultiLine(10, 40));
        assertThat(kind("Query {{text:rows=many}}", "java.lang.String"))
                .isEqualTo(new ParameterKind.MultiLine(ParameterKind.DEFAULT_ROWS, ParameterKind.DEFAULT_COLUMNS));
    }

    @Test
    public void aFileKeepsItsPatterns() {
        assertThat(kind("CSV {{file:*.csv,*.txt}}", "[B"))
                .isEqualTo(new ParameterKind.FileUpload(List.of("*.csv", "*.txt")));
    }

    @Test
    public void withoutAKnownTagTheTypeDecides() {
        assertThat(kind("Flag {{unknown:x}}", "boolean")).isEqualTo(new ParameterKind.Flag());
        assertThat(kind(null, "java.lang.Boolean")).isEqualTo(new ParameterKind.Flag());
        assertThat(kind("Count", "int")).isEqualTo(new ParameterKind.SingleLine());
    }

    @Test
    public void theTooltipIsTheDescriptionWithoutItsMarkup() {
        assertThat(ParameterKind.tooltip(parameter("Pair {{combo:A,B}} to trade", "java.lang.String")))
                .isEqualTo("Pair to trade");
        assertThat(ParameterKind.tooltip(parameter("Plain", "int"))).isEqualTo("Plain");
        assertThat(ParameterKind.tooltip(parameter(null, "int"))).isEmpty();
    }

    @Test
    public void theInputShowsTheSimpleTypeName() {
        assertThat(ParameterInputs.simpleName("java.lang.String")).isEqualTo("String");
        assertThat(ParameterInputs.simpleName("[Ljava.lang.String;")).isEqualTo("String[]");
        assertThat(ParameterInputs.simpleName("long")).isEqualTo("long");
    }

    @Test
    public void onlyAFormatThatWritesADateGetsAPicker() {
        assertThat(DateInput.formatter("dd.MM.yyyy")).isPresent();
        assertThat(DateInput.formatter("HH:mm")).isEmpty();
        assertThat(DateInput.formatter("")).isEmpty();
    }

    static MBeanParameterInfo parameter(String description, String type) {
        return new MBeanParameterInfo("p", type, description);
    }

    private static ParameterKind kind(String description, String type) {
        return ParameterKind.of(parameter(description, type));
    }
}
