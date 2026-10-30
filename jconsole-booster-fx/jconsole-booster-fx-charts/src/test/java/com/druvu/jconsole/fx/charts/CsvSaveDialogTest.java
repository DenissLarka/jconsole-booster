package com.druvu.jconsole.fx.charts;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import org.testng.annotations.Test;

/** Checks that a saved file always gets the CSV extension. */
public class CsvSaveDialogTest {

    @Test
    public void aNameWithoutTheExtensionGetsIt() {
        assertThat(CsvSaveDialog.withCsvExtension(new File("exports", "heap")))
                .isEqualTo(new File("exports", "heap.csv"));
    }

    @Test
    public void aNameWithTheExtensionInAnyCaseStays() {
        File file = new File("exports", "heap.CSV");

        assertThat(CsvSaveDialog.withCsvExtension(file)).isSameAs(file);
    }

    @Test
    public void anotherExtensionStaysBeforeTheCsvOne() {
        assertThat(CsvSaveDialog.withCsvExtension(new File("exports", "heap.txt")))
                .isEqualTo(new File("exports", "heap.txt.csv"));
    }
}
