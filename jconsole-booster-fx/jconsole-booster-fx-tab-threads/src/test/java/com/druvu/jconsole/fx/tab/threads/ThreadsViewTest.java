package com.druvu.jconsole.fx.tab.threads;

import static org.assertj.core.api.Assertions.assertThat;

import com.druvu.jconsole.core.text.Messages;
import com.druvu.jconsole.core.vm.JmxDataAccess;
import com.druvu.jconsole.fx.tab.TabContext;
import java.lang.management.ThreadMXBean;
import java.util.List;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/** The Threads view on the FX thread: the list and its filter, a thread's details, and the deadlock search. */
public class ThreadsViewTest {

    private static final ThreadEntry MAIN = new ThreadEntry(1, "main");
    private static final ThreadEntry FIRST_WORKER = new ThreadEntry(20, "worker-1");
    private static final ThreadEntry SECOND_WORKER = new ThreadEntry(21, "worker-2");

    @BeforeClass
    public void startFx() {
        FxTestToolkit.ensureStarted();
    }

    @Test
    public void showsTheThreadsAndTheirCount() {
        List<Object> shown = FxTestToolkit.call(() -> {
            ThreadsScreen screen = screen(new LocalJvmConnection(), new RecordingHost());
            return List.of(screen.names(0), screen.count());
        });

        assertThat(shown).containsExactly(List.of("main", "worker-1", "worker-2"), "Threads: 3");
    }

    @Test
    public void theFilterNarrowsTheListAndClearingItBringsTheThreadsBack() {
        List<Object> shown = FxTestToolkit.call(() -> {
            ThreadsScreen screen = screen(new LocalJvmConnection(), new RecordingHost());
            screen.filter().setText("WORKER");
            List<Object> filtered = List.of(screen.names(0), screen.count());
            screen.filter().setText("");
            return List.of(filtered, screen.names(0));
        });

        assertThat(shown)
                .containsExactly(
                        List.of(List.of("worker-1", "worker-2"), "Threads: 2 of 3"),
                        List.of("main", "worker-1", "worker-2"));
    }

    @Test
    public void aNewReadingKeepsTheSelectedThreadSelected() {
        ThreadEntry selected = FxTestToolkit.call(() -> {
            ThreadsView view = view(new LocalJvmConnection(), new RecordingHost());
            ThreadsScreen screen = new ThreadsScreen(view.root());
            view.show(listing(MAIN, FIRST_WORKER));
            screen.list(0).getSelectionModel().select(FIRST_WORKER);
            view.show(listing(FIRST_WORKER, SECOND_WORKER));
            return screen.list(0).getSelectionModel().getSelectedItem();
        });

        assertThat(selected).isEqualTo(FIRST_WORKER);
    }

    @Test
    public void selectingAThreadShowsItsDetails() {
        Thread waiting = Thread.currentThread();
        ThreadEntry entry = new ThreadEntry(waiting.threadId(), waiting.getName());
        ThreadsScreen screen = FxTestToolkit.call(() -> {
            ThreadsView view = view(new LocalJvmConnection(), new RecordingHost());
            view.show(listing(entry));
            return new ThreadsScreen(view.root());
        });
        assertThat(FxTestToolkit.call(() -> screen.details(0)))
                .isEqualTo(Messages.THREAD_TAB_INITIAL_STACK_TRACE_MESSAGE);

        FxTestToolkit.runAndWait(() -> screen.list(0).getSelectionModel().select(entry));

        // the details arrive in a later FX step
        assertThat(FxTestToolkit.call(() -> screen.details(0)))
                .startsWith("Name: " + waiting.getName() + "\n")
                .contains(ThreadsViewTest.class.getName());
    }

    @Test
    public void aThreadThatCannotBeReadSaysWhy() {
        ThreadMXBean lost = LostConnection.bean(ThreadMXBean.class);
        ThreadsScreen screen =
                FxTestToolkit.call(() -> screen(new LocalJvmConnection(lost, true), new RecordingHost()));

        FxTestToolkit.runAndWait(() -> screen.list(0).getSelectionModel().select(MAIN));

        assertThat(FxTestToolkit.call(() -> screen.details(0)))
                .isEqualTo(ThreadBrowser.READ_FAILED + LostConnection.MESSAGE);
    }

    @Test
    public void aDeadlockGetsATabOfItsOwnAndGoesWhenItIsOver() throws InterruptedException {
        ThreadsScreen screen = FxTestToolkit.call(() -> screen(new LocalJvmConnection(), new RecordingHost()));

        try (DeadlockRing ring = DeadlockRing.start("view-deadlock", 2)) {
            FxTestToolkit.runAndWait(screen::detectDeadlock);
            FxTestToolkit.runAndWait(screen::layout);

            assertThat(FxTestToolkit.call(screen::tabTitles)).containsExactly(Messages.THREADS, "Deadlock");
            assertThat(FxTestToolkit.call(screen::selectedTab)).isEqualTo(1);
            assertThat(FxTestToolkit.call(() -> screen.names(1))).containsExactlyInAnyOrderElementsOf(ring.names());
            // the first thread of the cycle is selected and its details read
            assertThat(FxTestToolkit.call(() -> screen.details(1))).contains(" owned by: view-deadlock-");
        }

        FxTestToolkit.runAndWait(screen::detectDeadlock);

        assertThat(FxTestToolkit.call(screen::tabTitles)).containsExactly(Messages.THREADS);
        assertThat(FxTestToolkit.call(screen::message)).isEqualTo(Messages.NO_DEADLOCK_DETECTED);
    }

    @Test
    public void aFailedDeadlockSearchIsReportedToTheHost() {
        RecordingHost host = new RecordingHost();
        JmxDataAccess lost = new LocalJvmConnection(LostConnection.bean(ThreadMXBean.class), true);
        ThreadsScreen screen = FxTestToolkit.call(() -> screen(lost, host));

        FxTestToolkit.runAndWait(screen::detectDeadlock);

        assertThat(FxTestToolkit.call(host::errorTitles)).containsExactly(Messages.DETECT_DEADLOCK);
        assertThat(FxTestToolkit.call(screen::message)).isEmpty();
    }

    /** A view showing main and two workers, in a scene. */
    private static ThreadsScreen screen(JmxDataAccess connection, RecordingHost host) {
        ThreadsView view = view(connection, host);
        view.show(listing(MAIN, FIRST_WORKER, SECOND_WORKER));
        return new ThreadsScreen(view.root());
    }

    /** A view whose background reads run at once, on the FX thread; their results still arrive in a later FX step. */
    private static ThreadsView view(JmxDataAccess connection, RecordingHost host) {
        return new ThreadsView(new TabContext(connection, host), new BackgroundReads(Runnable::run));
    }

    private static ThreadListing listing(ThreadEntry... threads) {
        return new ThreadListing(System.currentTimeMillis(), threads.length, threads.length, List.of(threads));
    }
}
