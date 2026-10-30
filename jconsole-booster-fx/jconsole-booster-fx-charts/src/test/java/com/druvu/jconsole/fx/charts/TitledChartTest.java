package com.druvu.jconsole.fx.charts;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import javafx.scene.layout.BorderPane;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/** Checks the titled chart: its title, and the info line that shows only with text. */
public class TitledChartTest {

    @BeforeClass
    public void startToolkit() {
        FxTestToolkit.ensureStarted();
    }

    @Test
    public void showsTheTitleAndDoesNotCollapse() {
        List<Object> state = FxTestToolkit.call(() -> {
            TitledChart titled = new TitledChart("Heap Memory Usage", new TimeSeriesChart());
            return List.of(titled.getText(), titled.isCollapsible());
        });

        assertThat(state).containsExactly("Heap Memory Usage", false);
    }

    @Test
    public void theInfoLineShowsOnlyWithText() {
        List<Boolean> visible = FxTestToolkit.call(() -> {
            TitledChart titled = new TitledChart("Threads", new TimeSeriesChart());
            boolean atFirst = infoVisible(titled);
            titled.setInfo("Live: 12");
            return List.of(atFirst, infoVisible(titled));
        });

        assertThat(visible).containsExactly(false, true);
    }

    private static boolean infoVisible(TitledChart titled) {
        if (!(titled.getContent() instanceof BorderPane content)) {
            throw new AssertionError("The content is not the chart's pane");
        }
        return content.getBottom().isVisible() && content.getBottom().isManaged();
    }
}
