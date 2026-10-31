package com.druvu.jconsole.fx.tab.classes;

import static org.assertj.core.api.Assertions.assertThat;

import com.druvu.jconsole.fx.tab.DetailRow;
import java.util.List;
import org.testng.annotations.Test;

/** The class loading details in words: the lines in the Swing JConsole's order, with their values. */
public class ClassDetailsTest {

    @Test
    public void listsTheTimeThenTheCounts() {
        ClassLoadingReading reading = new ClassLoadingReading(System.currentTimeMillis(), 700, 750, 50, false);

        List<DetailRow> rows = ClassDetails.rows(reading);

        assertThat(rows)
                .extracting(DetailRow::name)
                .containsExactly("Time", "Current classes loaded", "Total classes loaded", "Total classes unloaded");
        assertThat(rows.subList(1, 4)).extracting(DetailRow::value).containsExactly("700", "750", "50");
        assertThat(rows.getFirst().value()).matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}");
    }
}
