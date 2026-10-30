package com.druvu.jconsole.fx.tab.classes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.druvu.jconsole.fx.charts.TimeSeriesChart;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/** The Classes tab against this JVM: its place, a refresh that fills its page, the verbose switch, a lost link. */
public class ClassesTabTest {

    @BeforeClass
    public void startFx() {
        FxTestToolkit.ensureStarted();
    }

    @Test
    public void namesItselfClassesAtPlaceForty() {
        ClassesTab tab = new ClassesTab(LocalJvmConnection.local(), new RecordingHost());

        assertThat(tab.id()).isEqualTo("classes");
        assertThat(tab.title()).isEqualTo("Classes");
        assertThat(tab.place()).isEqualTo(40);
    }

    @Test
    public void aRefreshFillsThePage() throws IOException {
        ClassesTab tab = new ClassesTab(LocalJvmConnection.local(), new RecordingHost());
        Parent page = page(tab);

        FxTestToolkit.runAndWait(tab.refresh());

        TimeSeriesChart chart = FxTestToolkit.call(() -> (TimeSeriesChart) page.lookup(".time-series-chart"));
        assertThat(FxTestToolkit.call(() -> chart.lastValue(ClassesView.LOADED_KEY)))
                .isPresent();
        assertThat(FxTestToolkit.call(() -> chart.lastValue(ClassesView.TOTAL_LOADED_KEY)))
                .isPresent();
        assertThat(FxTestToolkit.call(() -> verboseSwitch(page).isSelected()))
                .isEqualTo(ManagementFactory.getClassLoadingMXBean().isVerbose());
        assertThat(FxTestToolkit.call(() -> detailTexts(page)))
                .contains("Time:", "Current classes loaded:", "Total classes loaded:", "Total classes unloaded:");
    }

    @Test
    public void aClickOnTheVerboseSwitchTurnsItOnInTheTarget() throws IOException, InterruptedException {
        CountDownLatch turnedOn = new CountDownLatch(1);
        ClassesTab tab = new ClassesTab(
                LocalJvmConnection.to(LocalJvmConnection.keepingVerbose(verbose -> {
                    if (verbose) {
                        turnedOn.countDown();
                    }
                })),
                new RecordingHost());
        Parent page = page(tab);
        FxTestToolkit.runAndWait(tab.refresh());

        FxTestToolkit.runAndWait(() -> verboseSwitch(page).fire());

        assertThat(turnedOn.await(10, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void setVerboseHandsTheStateToTheTarget() throws InterruptedException {
        AtomicBoolean given = new AtomicBoolean(true);
        RecordingHost host = new RecordingHost();
        ClassesTab tab = new ClassesTab(LocalJvmConnection.to(LocalJvmConnection.keepingVerbose(given::set)), host);

        tab.setVerbose(false).join();

        assertThat(given).isFalse();
        assertThat(host.errors()).isEmpty();
    }

    @Test
    public void aFailedVerboseChangeIsShownWithItsCause() throws InterruptedException {
        RecordingHost host = new RecordingHost();
        ClassesTab tab = new ClassesTab(LocalJvmConnection.to(LocalJvmConnection.lostBean()), host);

        tab.setVerbose(true).join();
        // the error reaches the app through the FX thread
        FxTestToolkit.runAndWait(() -> {});

        assertThat(host.errors()).containsExactly("Verbose Output: connection lost");
    }

    @Test
    public void aLostConnectionIsAnIoException() {
        ClassesTab tab = new ClassesTab(LocalJvmConnection.to(LocalJvmConnection.lostBean()), new RecordingHost());

        assertThatThrownBy(tab::refresh).isInstanceOf(IOException.class).hasMessage("connection lost");
    }

    /** The tab's page in a scene, so the controls have their skins and their parts can be looked up. */
    private static Parent page(ClassesTab tab) {
        return FxTestToolkit.call(() -> {
            Parent page = (Parent) tab.content();
            new Scene(page);
            page.applyCss();
            return page;
        });
    }

    private static CheckBox verboseSwitch(Parent page) {
        return (CheckBox) page.lookup("." + ClassesView.VERBOSE_STYLE_CLASS);
    }

    private static List<String> detailTexts(Parent page) {
        page.applyCss();
        return page.lookup("." + DetailsGrid.STYLE_CLASS).lookupAll(".label").stream()
                .map(label -> ((Label) label).getText())
                .toList();
    }
}
