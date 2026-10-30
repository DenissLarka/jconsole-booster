package com.druvu.jconsole.fx.tab.jtop;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/** The JTop table on the FX thread: a reading's rows and status, and the user's column sort kept across readings. */
public class JTopViewTest {

    private static final ThreadSnapshot FIRST_READING =
            new ThreadSnapshot(List.of(row("b", 3.0), row("c", 2.0), row("a", 1.0)), "Threads: 3");
    private static final ThreadSnapshot SECOND_READING =
            new ThreadSnapshot(List.of(row("c", 9.0), row("a", 5.0), row("b", 1.0)), "Threads: 3");

    @BeforeClass
    public void startFx() {
        FxTestToolkit.ensureStarted();
    }

    @Test
    public void showsTheRowsInTheReadingsOrder() {
        List<String> names = FxTestToolkit.call(() -> {
            JTopView view = new JTopView();
            view.show(FIRST_READING);
            view.show(SECOND_READING);
            return shownNames(view);
        });

        assertThat(names).containsExactly("c", "a", "b");
    }

    @Test
    public void showsTheStatus() {
        String status = FxTestToolkit.call(() -> {
            JTopView view = new JTopView();
            view.show(ThreadSnapshot.withoutRows(ThreadCpuReader.CPU_TIME_UNSUPPORTED));
            return ((Label) view.root().lookup("." + JTopView.STATUS_STYLE_CLASS)).getText();
        });

        assertThat(status).isEqualTo(ThreadCpuReader.CPU_TIME_UNSUPPORTED);
    }

    @Test
    public void keepsTheUsersSortAcrossReadings() {
        List<String> names = FxTestToolkit.call(() -> {
            JTopView view = new JTopView();
            view.show(FIRST_READING);
            sortByFirstColumn(table(view));
            view.show(SECOND_READING);
            return shownNames(view);
        });

        assertThat(names).containsExactly("a", "b", "c");
    }

    @Test
    public void showsCpuTimeWithFourDecimals() {
        assertThat(CpuSecondsCell.text(1.5)).matches("1[.,]5000");
    }

    private static ThreadRow row(String name, double cpuSeconds) {
        return new ThreadRow(name, cpuSeconds, Thread.State.RUNNABLE);
    }

    private static TableView<?> table(JTopView view) {
        return (TableView<?>) view.root().lookup(".table-view");
    }

    private static List<String> shownNames(JTopView view) {
        return table(view).getItems().stream()
                .map(item -> ((ThreadRow) item).name())
                .toList();
    }

    /** What a click on the first column's header does. */
    private static <S> void sortByFirstColumn(TableView<S> table) {
        table.getSortOrder().add(table.getColumns().getFirst());
    }
}
