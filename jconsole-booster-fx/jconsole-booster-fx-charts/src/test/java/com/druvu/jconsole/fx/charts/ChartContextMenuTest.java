package com.druvu.jconsole.fx.charts;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.RadioMenuItem;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/** Checks the chart's context menu: the time range items and the save item. */
public class ChartContextMenuTest {

    @BeforeClass
    public void startToolkit() {
        FxTestToolkit.ensureStarted();
    }

    @Test
    public void listsEveryRangeWithTheShownOneMarked() {
        ObjectProperty<TimeRange> range = new SimpleObjectProperty<>(TimeRange.ONE_HOUR);

        List<RadioMenuItem> items = FxTestToolkit.call(() -> rangeItems(ChartContextMenu.create(range, () -> {})));

        assertThat(items).hasSize(TimeRange.values().length);
        assertThat(items)
                .filteredOn(RadioMenuItem::isSelected)
                .extracting(RadioMenuItem::getText)
                .containsExactly("1 hour");
    }

    @Test
    public void pickingARangeItemSetsTheRange() {
        ObjectProperty<TimeRange> range = new SimpleObjectProperty<>(TimeRange.ALL);

        FxTestToolkit.runAndWait(() -> rangeItems(ChartContextMenu.create(range, () -> {}))
                .get(TimeRange.TEN_MINUTES.ordinal())
                .fire());

        assertThat(range.get()).isEqualTo(TimeRange.TEN_MINUTES);
    }

    @Test
    public void theMarkFollowsARangeSetElsewhere() {
        ObjectProperty<TimeRange> range = new SimpleObjectProperty<>(TimeRange.ALL);

        List<String> marked = FxTestToolkit.call(() -> {
            ContextMenu menu = ChartContextMenu.create(range, () -> {});
            range.set(TimeRange.ONE_DAY);
            return rangeItems(menu).stream()
                    .filter(RadioMenuItem::isSelected)
                    .map(RadioMenuItem::getText)
                    .toList();
        });

        assertThat(marked).containsExactly("1 day");
    }

    @Test
    public void theSaveItemRunsTheSave() {
        AtomicBoolean saved = new AtomicBoolean();

        FxTestToolkit.runAndWait(
                () -> ChartContextMenu.create(new SimpleObjectProperty<>(TimeRange.ALL), () -> saved.set(true))
                        .getItems()
                        .getLast()
                        .fire());

        assertThat(saved).isTrue();
    }

    private static List<RadioMenuItem> rangeItems(ContextMenu menu) {
        if (!(menu.getItems().getFirst() instanceof Menu rangeMenu)) {
            throw new AssertionError("The first item is not the time range menu");
        }
        return rangeMenu.getItems().stream()
                .filter(RadioMenuItem.class::isInstance)
                .map(RadioMenuItem.class::cast)
                .toList();
    }
}
