package com.druvu.jconsole.inspector.operations;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.testng.Assert;
import org.testng.annotations.Test;

public class ParameterHintTest {

    @Test
    public void noMarkupReturnsEmpty() {
        Assert.assertEquals(ParameterHint.parse(null), Optional.empty());
        Assert.assertEquals(ParameterHint.parse(""), Optional.empty());
        Assert.assertEquals(ParameterHint.parse("plain description"), Optional.empty());
        Assert.assertEquals(ParameterHint.parse("p1"), Optional.empty()); // default JMX-introspected name
    }

    @Test
    public void comboTagParses() {
        ParameterHint h = ParameterHint.parse("Currency pair {{combo:EURUSD,USDCHF,GBPUSD}}").orElseThrow();
        Assert.assertEquals(h.tag(), "combo");
        Assert.assertEquals(h.options(), "EURUSD,USDCHF,GBPUSD");
        Assert.assertEquals(h.prose(), "Currency pair");
        Assert.assertEquals(h.optionsAsList(), List.of("EURUSD", "USDCHF", "GBPUSD"));
    }

    @Test
    public void dateTagParses() {
        ParameterHint h = ParameterHint.parse("Settle date {{date:dd.MM.yyyy}}").orElseThrow();
        Assert.assertEquals(h.tag(), "date");
        Assert.assertEquals(h.options(), "dd.MM.yyyy");
        Assert.assertEquals(h.prose(), "Settle date");
    }

    @Test
    public void textTagWithRowsParses() {
        ParameterHint h = ParameterHint.parse("Payload {{text:rows=10}}").orElseThrow();
        Assert.assertEquals(h.tag(), "text");
        Assert.assertEquals(h.optionsAsKeyValue(), Map.of("rows", "10"));
        Assert.assertEquals(h.prose(), "Payload");
    }

    @Test
    public void textTagWithoutOptionsParses() {
        ParameterHint h = ParameterHint.parse("Multi-line {{text}}").orElseThrow();
        Assert.assertEquals(h.tag(), "text");
        Assert.assertEquals(h.options(), "");
        Assert.assertTrue(h.optionsAsKeyValue().isEmpty());
        Assert.assertEquals(h.prose(), "Multi-line");
    }

    @Test
    public void fileTagWithGlobsParses() {
        ParameterHint h = ParameterHint.parse("CSV file {{file:*.csv,*.tsv}}").orElseThrow();
        Assert.assertEquals(h.tag(), "file");
        Assert.assertEquals(h.optionsAsList(), List.of("*.csv", "*.tsv"));
    }

    @Test
    public void fileTagWithoutOptionsParses() {
        ParameterHint h = ParameterHint.parse("Any file {{file}}").orElseThrow();
        Assert.assertEquals(h.tag(), "file");
        Assert.assertTrue(h.optionsAsList().isEmpty());
    }

    @Test
    public void returnsTagParses() {
        ParameterHint h = ParameterHint.parse("Server config {{returns:format=json}}").orElseThrow();
        Assert.assertEquals(h.tag(), "returns");
        Assert.assertEquals(h.optionsAsKeyValue(), Map.of("format", "json"));
        Assert.assertEquals(h.prose(), "Server config");
    }

    @Test
    public void returnsMimeParses() {
        ParameterHint h = ParameterHint.parse("Report {{returns:mime=application/pdf}}").orElseThrow();
        Assert.assertEquals(h.optionsAsKeyValue(), Map.of("mime", "application/pdf"));
    }

    @Test
    public void emptyOptionsIsTreatedAsNoOptions() {
        ParameterHint h = ParameterHint.parse("X {{combo:}}").orElseThrow();
        Assert.assertEquals(h.tag(), "combo");
        Assert.assertEquals(h.options(), "");
        Assert.assertTrue(h.optionsAsList().isEmpty());
    }

    @Test
    public void onlyFirstMarkupIsHonored() {
        // Plan: at most one markup tag per description; the rest should be ignored.
        ParameterHint h = ParameterHint.parse("X {{combo:A,B}} Y {{date:yyyy}}").orElseThrow();
        Assert.assertEquals(h.tag(), "combo");
        Assert.assertEquals(h.options(), "A,B");
    }

    @Test
    public void prosePreservesSurroundingText() {
        ParameterHint h = ParameterHint.parse("before {{combo:A,B}} after").orElseThrow();
        Assert.assertEquals(h.prose(), "before after");
    }

    @Test
    public void caseInsensitiveTag() {
        ParameterHint h = ParameterHint.parse("X {{ComBo:A,B}}").orElseThrow();
        Assert.assertEquals(h.tag(), "combo");
    }

    @Test
    public void unknownTagStillParses() {
        // Unknown tags are returned as-is; consumers (factory, result handler) decide what to do.
        ParameterHint h = ParameterHint.parse("X {{slider:1..100}}").orElseThrow();
        Assert.assertEquals(h.tag(), "slider");
        Assert.assertEquals(h.options(), "1..100");
    }
}
