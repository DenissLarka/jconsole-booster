package com.druvu.jconsole.fx.tab.memory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.druvu.jconsole.fx.charts.TimeSeriesChart;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/** The Memory tab against this JVM: its place, a refresh that fills its page, the chart chooser, GC, a lost link. */
public class MemoryTabTest {

    @BeforeClass
    public void startFx() {
        FxTestToolkit.ensureStarted();
    }

    @Test
    public void namesItselfMemoryAtPlaceTwenty() {
        MemoryTab tab = new MemoryTab(new LocalJvmConnection().connection(), new RecordingHost());

        assertThat(tab.id()).isEqualTo("memory");
        assertThat(tab.title()).isEqualTo("Memory");
        assertThat(tab.place()).isEqualTo(20);
    }

    @Test
    public void aRefreshFillsThePage() throws IOException {
        MemoryTab tab = new MemoryTab(new LocalJvmConnection().connection(), new RecordingHost());
        Parent page = page(tab);

        FxTestToolkit.runAndWait(tab.refresh());

        List<MemoryArea> entries =
                FxTestToolkit.call(() -> List.copyOf(chooser(page).getItems()));
        int poolCount = ManagementFactory.getMemoryPoolMXBeans().size();
        assertThat(entries).startsWith(MemoryArea.HEAP, MemoryArea.NON_HEAP).hasSize(2 + poolCount);
        assertThat(FxTestToolkit.call(() -> chooser(page).getValue())).isEqualTo(MemoryArea.HEAP);
        assertThat(FxTestToolkit.call(
                        () -> page.lookupAll("." + PoolBar.STYLE_CLASS).size()))
                .isEqualTo(poolCount);
        assertThat(FxTestToolkit.call(() -> detailTexts(page))).contains("Time:", "Used:", "GC time:");
        assertThat(FxTestToolkit.call(() -> shownChart(page).lastValue(UsageChart.USED_KEY)))
                .isPresent();
    }

    @Test
    public void eachAreaKeepsItsChartWhileAnotherIsShown() throws IOException {
        MemoryTab tab = new MemoryTab(new LocalJvmConnection().connection(), new RecordingHost());
        Parent page = page(tab);
        FxTestToolkit.runAndWait(tab.refresh());
        TimeSeriesChart heapChart = FxTestToolkit.call(() -> shownChart(page));

        FxTestToolkit.runAndWait(() -> chooser(page).setValue(MemoryArea.NON_HEAP));
        TimeSeriesChart nonHeapChart = FxTestToolkit.call(() -> shownChart(page));
        FxTestToolkit.runAndWait(tab.refresh());
        FxTestToolkit.runAndWait(() -> chooser(page).setValue(MemoryArea.HEAP));

        assertThat(nonHeapChart).isNotSameAs(heapChart);
        assertThat(FxTestToolkit.call(() -> shownChart(page))).isSameAs(heapChart);
        assertThat(FxTestToolkit.call(heapChart::lastTime)).isEqualTo(FxTestToolkit.call(nonHeapChart::lastTime));
    }

    @Test
    public void performGcAsksTheTargetToCollect() throws InterruptedException {
        AtomicBoolean collected = new AtomicBoolean();
        MemoryMXBean memory =
                LocalJvmConnection.replacing(MemoryMXBean.class, ManagementFactory.getMemoryMXBean(), "gc", args -> {
                    collected.set(true);
                    return null;
                });
        LocalJvmConnection connection = new LocalJvmConnection().answer("getMemoryMXBean", args -> memory);
        RecordingHost host = new RecordingHost();

        new MemoryTab(connection.connection(), host).performGc().join();

        assertThat(collected).isTrue();
        assertThat(host.errors()).isEmpty();
    }

    @Test
    public void aFailedGcIsShownWithItsCause() throws InterruptedException {
        LocalJvmConnection connection = new LocalJvmConnection()
                .answer("getMemoryMXBean", args -> LocalJvmConnection.lostBean(MemoryMXBean.class));
        RecordingHost host = new RecordingHost();

        new MemoryTab(connection.connection(), host).performGc().join();
        // the error reaches the app through the FX thread
        FxTestToolkit.runAndWait(() -> {});

        assertThat(host.errors()).containsExactly("Perform GC: connection lost");
    }

    @Test
    public void aLostConnectionIsAnIoException() {
        LocalJvmConnection connection = new LocalJvmConnection()
                .answer("getMemoryMXBean", args -> LocalJvmConnection.lostBean(MemoryMXBean.class));
        MemoryTab tab = new MemoryTab(connection.connection(), new RecordingHost());

        assertThatThrownBy(tab::refresh).isInstanceOf(IOException.class).hasMessage("connection lost");
    }

    /** The tab's page in a scene, so the controls have their skins and their parts can be looked up. */
    private static Parent page(MemoryTab tab) {
        return FxTestToolkit.call(() -> {
            Parent page = (Parent) tab.content();
            new Scene(page);
            page.applyCss();
            return page;
        });
    }

    @SuppressWarnings("unchecked")
    private static ComboBox<MemoryArea> chooser(Node page) {
        return (ComboBox<MemoryArea>) page.lookup("." + MemoryView.CHOOSER_STYLE_CLASS);
    }

    private static TimeSeriesChart shownChart(Node page) {
        return (TimeSeriesChart) page.lookup(".time-series-chart");
    }

    private static List<String> detailTexts(Parent page) {
        page.applyCss();
        return page.lookup("." + DetailsGrid.STYLE_CLASS).lookupAll(".label").stream()
                .map(label -> ((Label) label).getText())
                .toList();
    }
}
