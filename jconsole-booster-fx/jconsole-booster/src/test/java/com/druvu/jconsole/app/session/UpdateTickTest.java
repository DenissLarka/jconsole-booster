package com.druvu.jconsole.app.session;

import static org.assertj.core.api.Assertions.assertThat;

import com.druvu.jconsole.fx.tab.ConnectionTab;
import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.scene.Node;
import org.testng.annotations.Test;

/**
 * Covers one update tick: flush then refresh and step per tab, a pending step skips its tab, a failing tab is reported
 * once and dropped, a lost connection stops the tick, and nothing is reported after stop.
 */
public class UpdateTickTest {

    @Test
    public void aTickFlushesThenRefreshesEveryTabAndRunsItsStep() {
        AtomicInteger flushes = new AtomicInteger();
        FakeTab summary = new FakeTab("summary", 50);
        FakeTab mbeans = new FakeTab("mbeans", 90);
        Reports reports = new Reports();

        new UpdateTick(flushes::incrementAndGet, List.of(summary, mbeans), Runnable::run, reports).tick();

        assertThat(flushes.get()).isEqualTo(1);
        assertThat(List.of(summary.steps(), mbeans.steps())).containsExactly(1, 1);
        assertThat(reports.updates).isEqualTo(1);
    }

    @Test
    public void aTabWhoseStepHasNotRunWaitsForTheNextTick() {
        FakeTab summary = new FakeTab("summary", 50);
        List<Runnable> fxQueue = new ArrayList<>();
        UpdateTick tick = new UpdateTick(() -> {}, List.of(summary), fxQueue::add, new Reports());

        tick.tick();
        tick.tick();
        assertThat(summary.refreshes()).isEqualTo(1);

        runAll(fxQueue);
        tick.tick();
        assertThat(summary.refreshes()).isEqualTo(2);
    }

    @Test
    public void aFailingTabIsReportedOnceAndNoLongerRefreshed() {
        FakeTab broken = new FakeTab("broken", 10).failNextRefreshWith(new IllegalStateException("bug"));
        FakeTab summary = new FakeTab("summary", 50);
        Reports reports = new Reports();
        UpdateTick tick = new UpdateTick(() -> {}, List.of(broken, summary), Runnable::run, reports);

        tick.tick();
        tick.tick();

        assertThat(reports.failedTabs).containsExactly(broken);
        assertThat(broken.refreshes()).isEqualTo(1);
        assertThat(summary.steps()).isEqualTo(2);
        assertThat(reports.lost).isEmpty();
    }

    @Test
    public void aStepThatThrowsFailsItsTab() {
        ConnectionTab brokenStep = new StepFailingTab();
        Reports reports = new Reports();
        UpdateTick tick = new UpdateTick(() -> {}, List.of(brokenStep), Runnable::run, reports);

        tick.tick();
        tick.tick();

        assertThat(reports.failedTabs).containsExactly(brokenStep);
    }

    @Test
    public void anIoExceptionIsALostConnectionAndStopsTheTick() {
        FakeTab summary = new FakeTab("summary", 50).failNextRefreshWith(new IOException("connection closed"));
        FakeTab mbeans = new FakeTab("mbeans", 90);
        Reports reports = new Reports();
        UpdateTick tick = new UpdateTick(() -> {}, List.of(summary, mbeans), Runnable::run, reports);

        tick.tick();
        tick.tick();

        assertThat(reports.lost).hasSize(1);
        assertThat(mbeans.refreshes()).isZero();
        assertThat(reports.failedTabs).isEmpty();
    }

    @Test
    public void anIoExceptionInsideAnMxBeanProxyIsALostConnectionToo() {
        FakeTab summary = new FakeTab("summary", 50)
                .failNextRefreshWith(new UndeclaredThrowableException(new IOException("connection closed")));
        Reports reports = new Reports();

        new UpdateTick(() -> {}, List.of(summary), Runnable::run, reports).tick();

        assertThat(reports.lost).hasSize(1);
        assertThat(reports.failedTabs).isEmpty();
    }

    @Test
    public void anUndeclaredExceptionWithoutAnIoCauseIsTheTabsFailure() {
        FakeTab summary = new FakeTab("summary", 50)
                .failNextRefreshWith(new UndeclaredThrowableException(new Exception("no such attribute")));
        Reports reports = new Reports();

        new UpdateTick(() -> {}, List.of(summary), Runnable::run, reports).tick();

        assertThat(reports.failedTabs).containsExactly(summary);
        assertThat(reports.lost).isEmpty();
    }

    @Test
    public void afterStopNothingIsReportedAndNoStepRuns() {
        FakeTab summary = new FakeTab("summary", 50);
        List<Runnable> fxQueue = new ArrayList<>();
        Reports reports = new Reports();
        UpdateTick tick = new UpdateTick(() -> {}, List.of(summary), fxQueue::add, reports);

        tick.tick();
        tick.stop();
        runAll(fxQueue);
        tick.tick();

        assertThat(summary.steps()).isZero();
        assertThat(summary.refreshes()).isEqualTo(1);
        assertThat(reports.updates).isZero();
    }

    private static void runAll(List<Runnable> fxQueue) {
        List<Runnable> queued = List.copyOf(fxQueue);
        fxQueue.clear();
        queued.forEach(Runnable::run);
    }

    /** A tab whose refresh works and whose step throws. */
    private static final class StepFailingTab implements ConnectionTab {

        @Override
        public String id() {
            return "step";
        }

        @Override
        public String title() {
            return "Step";
        }

        @Override
        public int place() {
            return 10;
        }

        @Override
        public Node content() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Runnable refresh() {
            return () -> {
                throw new IllegalStateException("the step is broken");
            };
        }
    }

    /** What the tick reported. */
    private static final class Reports implements UpdateTick.Listener {

        private final List<ConnectionTab> failedTabs = new ArrayList<>();
        private final List<Exception> lost = new ArrayList<>();
        private int updates;

        @Override
        public void tabFailed(ConnectionTab tab, RuntimeException failure) {
            failedTabs.add(tab);
        }

        @Override
        public void connectionLost(Exception cause) {
            lost.add(cause);
        }

        @Override
        public void updated(LocalTime at) {
            updates++;
        }
    }
}
