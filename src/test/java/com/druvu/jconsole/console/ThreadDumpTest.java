package com.druvu.jconsole.console;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import org.testng.annotations.Test;

/**
 * Unit coverage for the portable ({@code dumpAllThreads}) side of {@link ThreadDump} — the jcmd side is exercised
 * end-to-end in {@code ConsoleMainE2ETest}. Rows are taken from this JVM's own {@code ThreadMXBean} through the real
 * MBean server, so they are genuine {@code CompositeData}, not hand-built fixtures.
 */
public class ThreadDumpTest {

    private static final LocalDateTime STAMP = LocalDateTime.of(2026, 8, 24, 13, 30, 0);

    @Test
    public void formatsThreadsInJstackShape() throws Exception {
        String text = ThreadDump.format(localThreads(), null, STAMP);

        assertThat(text).startsWith("2026-08-24 13:30:00\n");
        assertThat(text).contains("Full thread dump");
        assertThat(text).contains("assembled by JConsoleBooster");
        // Every thread block carries the marker line every dump analyzer keys on.
        assertThat(text).contains("   java.lang.Thread.State: ");
        assertThat(text).contains("Locked ownable synchronizers:");
        // The test thread itself must be present, quoted and numbered.
        assertThat(text).contains('"' + Thread.currentThread().getName() + '"');
        assertThat(text).contains("\tat ");
    }

    @Test
    public void keepsStackFramesBeyondTheThreadInfoToStringCap() throws Exception {
        // ThreadInfo.toString() truncates at 8 frames; a real dump must not. deepStack() guarantees more than that.
        CompositeData[] rows = deepStack(16);

        String text = ThreadDump.format(rows, null, STAMP);

        long frames = text.lines().filter(l -> l.startsWith("\tat ")).count();
        assertThat(frames).isGreaterThan(8L);
        assertThat(text).doesNotContain("..."); // ThreadInfo.toString()'s truncation marker
    }

    @Test
    public void reportsDeadlockedIdsWhenPresent() throws Exception {
        String text = ThreadDump.format(localThreads(), new long[] {41L, 42L}, STAMP);

        assertThat(text).contains("Found 2 deadlocked thread(s), by id: 41 42");
    }

    @Test
    public void omitsTheDeadlockSectionWhenThereIsNone() throws Exception {
        assertThat(ThreadDump.format(localThreads(), null, STAMP)).doesNotContain("deadlocked");
        assertThat(ThreadDump.format(localThreads(), new long[0], STAMP)).doesNotContain("deadlocked");
    }

    @Test
    public void buildsATimestampedFileNameFromTheTarget() {
        assertThat(ThreadDump.defaultFileName("localhost:7091", STAMP))
                .isEqualTo("threaddump-localhost-7091-20260824-133000.txt");
    }

    @Test
    public void sanitizesFileNamesThatWouldBeIllegalOnDisk() {
        // ':' and '/' are illegal (Windows) or path-splitting (POSIX) — a dump must never land outside the cwd.
        assertThat(ThreadDump.defaultFileName("service:jmx:rmi:///jndi/rmi://host:1099/jmxrmi", STAMP))
                .doesNotContain(":")
                .doesNotContain("/")
                .startsWith("threaddump-service-jmx-rmi-jndi-rmi-host-1099");

        assertThat(ThreadDump.defaultFileName(null, STAMP)).isEqualTo("threaddump-target-20260824-133000.txt");
        assertThat(ThreadDump.defaultFileName("   ", STAMP)).isEqualTo("threaddump-target-20260824-133000.txt");
        assertThat(ThreadDump.defaultFileName("!!!", STAMP)).isEqualTo("threaddump-target-20260824-133000.txt");
    }

    // ----- fixtures -----

    /** This JVM's threads as real {@code CompositeData}, exactly as a remote {@code dumpAllThreads} would deliver. */
    private static CompositeData[] localThreads() throws Exception {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        return (CompositeData[]) mbs.invoke(
                ObjectName.getInstance("java.lang:type=Threading"),
                "dumpAllThreads",
                new Object[] {Boolean.TRUE, Boolean.TRUE},
                new String[] {"boolean", "boolean"});
    }

    /** One thread parked at the bottom of a {@code depth}-deep recursion, captured as a single dump row. */
    private static CompositeData[] deepStack(int depth) throws Exception {
        CountDownLatch parked = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        Thread t = new Thread(() -> recurse(depth, parked, release), "deep-stack-fixture");
        t.setDaemon(true);
        t.start();
        try {
            assertThat(parked.await(10, TimeUnit.SECONDS)).isTrue();
            ThreadInfo info = ManagementFactory.getThreadMXBean().getThreadInfo(t.threadId(), Integer.MAX_VALUE);
            return new CompositeData[] {toCompositeData(info)};
        } finally {
            release.countDown();
        }
    }

    private static void recurse(int remaining, CountDownLatch parked, CountDownLatch release) {
        if (remaining > 0) {
            recurse(remaining - 1, parked, release);
            return;
        }
        parked.countDown();
        try {
            release.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Round-trips a {@link ThreadInfo} back to the {@link CompositeData} shape {@code ThreadInfo.from} accepts, by
     * asking the MBean server for the same thread — the platform server does the conversion, so the fixture stays
     * faithful to the wire format instead of hand-rolling a {@link CompositeDataSupport}.
     */
    private static CompositeData toCompositeData(ThreadInfo info) throws Exception {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        return (CompositeData) mbs.invoke(
                ObjectName.getInstance("java.lang:type=Threading"),
                "getThreadInfo",
                new Object[] {info.getThreadId(), Integer.MAX_VALUE},
                new String[] {"long", "int"});
    }
}
