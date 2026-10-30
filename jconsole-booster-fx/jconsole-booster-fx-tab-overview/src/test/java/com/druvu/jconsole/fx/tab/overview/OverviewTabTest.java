package com.druvu.jconsole.fx.tab.overview;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.druvu.jconsole.core.text.Messages;
import com.druvu.jconsole.core.vm.JmxDataAccess;
import com.druvu.jconsole.fx.charts.TimeSeriesChart;
import com.druvu.jconsole.fx.charts.TitledChart;
import java.io.IOException;
import java.lang.management.MemoryMXBean;
import java.util.List;
import java.util.OptionalLong;
import javafx.scene.Node;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/** The Overview tab against this JVM: its place among the tabs, refreshes that fill its charts, a lost connection. */
public class OverviewTabTest {

    @BeforeClass
    public void startFx() {
        FxTestToolkit.ensureStarted();
    }

    @Test
    public void namesItselfOverviewAtPlaceTen() {
        OverviewTab tab = tab(new LocalJvmConnection());

        assertThat(tab.id()).isEqualTo("overview");
        assertThat(tab.title()).isEqualTo(Messages.OVERVIEW);
        assertThat(tab.place()).isEqualTo(10);
    }

    @Test
    public void showsFourChartsInOrder() {
        OverviewTab tab = tab(new LocalJvmConnection());

        List<String> titles = FxTestToolkit.call(() -> tab.content().lookupAll(".titled-pane").stream()
                .map(pane -> ((TitledChart) pane).getText())
                .toList());

        assertThat(titles)
                .containsExactly(Messages.HEAP_MEMORY_USAGE, Messages.THREADS, Messages.CLASSES, Messages.CPU_USAGE);
    }

    @Test
    public void theFirstRefreshChartsAllButTheCpuUsage() throws IOException {
        OverviewTab tab = tab(new LocalJvmConnection());
        Node content = FxTestToolkit.call(tab::content);

        FxTestToolkit.runAndWait(tab.refresh());

        assertThat(FxTestToolkit.call(() -> info(content, Messages.HEAP_MEMORY_USAGE)))
                .startsWith("Used: ");
        assertThat(FxTestToolkit.call(() -> info(content, Messages.THREADS))).startsWith("Live: ");
        assertThat(FxTestToolkit.call(() -> info(content, Messages.CLASSES))).startsWith("Loaded: ");
        assertThat(FxTestToolkit.call(() -> info(content, Messages.CPU_USAGE))).isEmpty();
        assertThat(FxTestToolkit.call(() -> lastValue(content, Messages.HEAP_MEMORY_USAGE, OverviewView.HEAP_KEY)))
                .isPresent();
        assertThat(FxTestToolkit.call(() -> lastValue(content, Messages.THREADS, OverviewView.THREADS_KEY)))
                .isPresent();
        assertThat(FxTestToolkit.call(() -> lastValue(content, Messages.CLASSES, OverviewView.CLASSES_KEY)))
                .isPresent();
        assertThat(FxTestToolkit.call(tab::content)).isSameAs(content);
    }

    @Test
    public void theSecondRefreshChartsTheCpuUsage() throws IOException {
        OverviewTab tab = tab(new LocalJvmConnection());
        Node content = FxTestToolkit.call(tab::content);

        FxTestToolkit.runAndWait(tab.refresh());
        OverviewReaderTest.letUptimeMove();
        FxTestToolkit.runAndWait(tab.refresh());

        assertThat(FxTestToolkit.call(() -> info(content, Messages.CPU_USAGE))).startsWith("CPU Usage: ");
        assertThat(FxTestToolkit.call(() -> lastValue(content, Messages.CPU_USAGE, OverviewView.CPU_KEY)))
                .isPresent();
    }

    @Test
    public void aLostConnectionIsAnIoException() {
        OverviewTab tab = tab(new LocalJvmConnection(LostConnection.bean(MemoryMXBean.class), true));

        assertThatThrownBy(tab::refresh).isInstanceOf(IOException.class).hasMessage(LostConnection.MESSAGE);
    }

    @Test
    public void aTargetWithoutPlatformBeansShowsNothing() throws IOException {
        OverviewTab tab = tab(new LocalJvmConnection(null, true));
        Node content = FxTestToolkit.call(tab::content);

        FxTestToolkit.runAndWait(tab.refresh());

        assertThat(FxTestToolkit.call(() -> info(content, Messages.HEAP_MEMORY_USAGE)))
                .isEmpty();
    }

    private static OverviewTab tab(JmxDataAccess connection) {
        return new OverviewTab(new OverviewReader(connection));
    }

    private static String info(Node content, String title) {
        return pane(content, title).getInfo();
    }

    private static OptionalLong lastValue(Node content, String title, String key) {
        return ((TimeSeriesChart) pane(content, title).getContent().lookup(".time-series-chart")).lastValue(key);
    }

    private static TitledChart pane(Node content, String title) {
        return content.lookupAll(".titled-pane").stream()
                .map(TitledChart.class::cast)
                .filter(pane -> pane.getText().equals(title))
                .findFirst()
                .orElseThrow();
    }
}
