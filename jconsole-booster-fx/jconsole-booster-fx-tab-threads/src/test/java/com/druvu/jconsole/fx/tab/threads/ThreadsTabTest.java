package com.druvu.jconsole.fx.tab.threads;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.druvu.jconsole.core.text.Messages;
import com.druvu.jconsole.core.vm.JmxDataAccess;
import com.druvu.jconsole.fx.tab.TabContext;
import java.io.IOException;
import java.lang.management.ThreadMXBean;
import java.util.OptionalLong;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/** The Threads tab against this JVM: its place among the tabs, a refresh that fills its content, a lost connection. */
public class ThreadsTabTest {

    @BeforeClass
    public void startFx() {
        FxTestToolkit.ensureStarted();
    }

    @Test
    public void namesItselfThreadsAtPlaceThirty() {
        ThreadsTab tab = tab(new LocalJvmConnection());

        assertThat(tab.id()).isEqualTo("threads");
        assertThat(tab.title()).isEqualTo(Messages.THREADS);
        assertThat(tab.place()).isEqualTo(30);
    }

    @Test
    public void aRefreshChartsTheCountsAndListsTheThreads() throws IOException {
        ThreadsTab tab = tab(new LocalJvmConnection());
        ThreadsScreen screen = FxTestToolkit.call(() -> new ThreadsScreen(tab.content()));

        FxTestToolkit.runAndWait(tab.refresh());

        OptionalLong live = FxTestToolkit.call(() -> screen.chart().lastValue(ThreadsView.LIVE_KEY));
        OptionalLong peak = FxTestToolkit.call(() -> screen.chart().lastValue(ThreadsView.PEAK_KEY));
        assertThat(live).isPresent();
        assertThat(peak.orElseThrow()).isGreaterThanOrEqualTo(live.getAsLong());
        assertThat(FxTestToolkit.call(() -> screen.names(0)))
                .contains(Thread.currentThread().getName());
        assertThat(FxTestToolkit.call(screen::count)).startsWith("Threads: ");
        assertThat(FxTestToolkit.call(tab::content)).isSameAs(FxTestToolkit.call(tab::content));
    }

    @Test
    public void aLostConnectionIsAnIoException() {
        ThreadsTab tab = tab(new LocalJvmConnection(LostConnection.bean(ThreadMXBean.class), true));

        assertThatThrownBy(tab::refresh).isInstanceOf(IOException.class).hasMessage(LostConnection.MESSAGE);
    }

    @Test
    public void aTargetWithoutPlatformBeansShowsNothing() throws IOException {
        ThreadsTab tab = tab(new LocalJvmConnection(null, true));
        ThreadsScreen screen = FxTestToolkit.call(() -> new ThreadsScreen(tab.content()));

        FxTestToolkit.runAndWait(tab.refresh());

        assertThat(FxTestToolkit.call(() -> screen.names(0))).isEmpty();
    }

    private static ThreadsTab tab(JmxDataAccess connection) {
        return new ThreadsTab(new TabContext(connection, new RecordingHost()), new BackgroundReads(Runnable::run));
    }
}
