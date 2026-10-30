package com.druvu.jconsole.fx.tab.threads;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.Comparator;
import java.util.concurrent.CountDownLatch;
import org.testng.annotations.Test;

/** Reading this JVM's threads: the counts, the threads oldest first, and the names kept from the first reading. */
public class ThreadListReaderTest {

    private final ThreadMXBean threads = ManagementFactory.getThreadMXBean();

    @Test
    public void readsTheCountsAndTheThreadsOldestFirst() {
        ThreadListing listing = new ThreadListReader().read(threads);

        assertThat(listing.live()).isPositive();
        assertThat(listing.peak()).isGreaterThanOrEqualTo(listing.live());
        assertThat(listing.timeMillis()).isPositive();
        assertThat(listing.threads())
                .isSortedAccordingTo(Comparator.comparingLong(ThreadEntry::id))
                .contains(new ThreadEntry(
                        Thread.currentThread().threadId(),
                        Thread.currentThread().getName()));
    }

    @Test
    public void aNewThreadShowsUpAndLeavesWhenItEnds() throws InterruptedException {
        ThreadListReader reader = new ThreadListReader();
        CountDownLatch release = new CountDownLatch(1);
        Thread waiting = new Thread(() -> awaitQuietly(release), "list-reader-waiting");
        ThreadEntry entry = new ThreadEntry(waiting.threadId(), waiting.getName());

        assertThat(reader.read(threads).threads()).doesNotContain(entry);
        waiting.start();
        assertThat(reader.read(threads).threads()).contains(entry);
        release.countDown();
        waiting.join();
        assertThat(reader.read(threads).threads()).doesNotContain(entry);
    }

    @Test
    public void keepsTheNameReadFirst() throws InterruptedException {
        ThreadListReader reader = new ThreadListReader();
        CountDownLatch release = new CountDownLatch(1);
        Thread renamed = new Thread(() -> awaitQuietly(release), "list-reader-first-name");
        renamed.start();
        try {
            reader.read(threads);
            renamed.setName("list-reader-second-name");

            assertThat(reader.read(threads).threads())
                    .contains(new ThreadEntry(renamed.threadId(), "list-reader-first-name"));
        } finally {
            release.countDown();
            renamed.join();
        }
    }

    private static void awaitQuietly(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }
}
