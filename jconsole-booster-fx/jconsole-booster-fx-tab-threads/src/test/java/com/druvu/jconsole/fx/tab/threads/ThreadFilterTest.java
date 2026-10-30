package com.druvu.jconsole.fx.tab.threads;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.testng.annotations.Test;

/** The thread list's filter: what it keeps, in which order, and the count line beside it. */
public class ThreadFilterTest {

    private static final ThreadEntry MAIN = new ThreadEntry(1, "main");
    private static final ThreadEntry FIRST_WORKER = new ThreadEntry(2, "pool-1-Worker-1");
    private static final ThreadEntry SECOND_WORKER = new ThreadEntry(3, "pool-1-worker-2");
    private static final List<ThreadEntry> THREADS = List.of(MAIN, FIRST_WORKER, SECOND_WORKER);

    @Test
    public void anEmptyFilterKeepsEveryThread() {
        assertThat(ThreadFilter.apply(THREADS, "")).isEqualTo(THREADS);
    }

    @Test
    public void keepsTheNamesHoldingTheTextInAnyCase() {
        assertThat(ThreadFilter.apply(THREADS, "WORKER")).containsExactly(FIRST_WORKER, SECOND_WORKER);
    }

    @Test
    public void keepsNothingWhenNoNameMatches() {
        assertThat(ThreadFilter.apply(THREADS, "gc")).isEmpty();
    }

    @Test
    public void countsTheThreadsShownAndOfHowMany() {
        assertThat(ThreadFilter.countText(3, 3)).isEqualTo("Threads: 3");
        assertThat(ThreadFilter.countText(2, 3)).isEqualTo("Threads: 2 of 3");
    }
}
