package com.druvu.jconsole.fx.tab.mbeans.viewers;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.testng.annotations.Test;

/** CSV as spreadsheets read it. */
public class CsvTest {

    @Test
    public void aCellIsQuotedOnlyWhenItMustBe() {
        assertThat(Csv.escape("plain")).isEqualTo("plain");
        assertThat(Csv.escape("a,b")).isEqualTo("\"a,b\"");
        assertThat(Csv.escape("say \"hi\"")).isEqualTo("\"say \"\"hi\"\"\"");
        assertThat(Csv.escape("two\nlines")).isEqualTo("\"two\nlines\"");
    }

    @Test
    public void linesEndWithCrLf() {
        assertThat(Csv.of(List.of("a", "b"), List.of(List.of("1", "2")))).isEqualTo("a,b\r\n1,2\r\n");
    }
}
