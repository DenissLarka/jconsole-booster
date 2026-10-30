package com.druvu.jconsole.fx.tab.jtop;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.testng.annotations.Test;

/** Reading this JVM's own threads: the rows, their order, and the status when CPU time cannot be measured. */
public class ThreadCpuReaderTest {

    private static final long BUSY_CPU_NANOS = TimeUnit.MILLISECONDS.toNanos(50);

    @Test
    public void readsTheLiveThreadsBusiestFirst() {
        ThreadSnapshot snapshot = ThreadCpuReader.read(ManagementFactory.getThreadMXBean());

        assertThat(snapshot.rows()).isNotEmpty().isSortedAccordingTo(ThreadRow.BUSIEST_FIRST);
        assertThat(snapshot.rows())
                .extracting(ThreadRow::name)
                .contains(Thread.currentThread().getName());
        assertThat(snapshot.rows())
                .allSatisfy(row -> assertThat(row.cpuSeconds()).isNotNegative());
        assertThat(snapshot.status()).isEqualTo("Threads: " + snapshot.rows().size());
    }

    @Test
    public void aBusyThreadComesBeforeAnIdleOne() throws InterruptedException {
        CountDownLatch busyDone = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        Thread busy = new Thread(
                () -> {
                    burnCpu();
                    busyDone.countDown();
                    await(release);
                },
                "jtop-test-busy");
        Thread idle = new Thread(() -> await(release), "jtop-test-idle");
        busy.start();
        idle.start();
        try {
            assertThat(busyDone.await(10, TimeUnit.SECONDS)).isTrue();

            ThreadSnapshot snapshot = ThreadCpuReader.read(ManagementFactory.getThreadMXBean());

            assertThat(snapshot.rows())
                    .extracting(ThreadRow::name)
                    .containsSubsequence("jtop-test-busy", "jtop-test-idle");
        } finally {
            release.countDown();
            busy.join();
            idle.join();
        }
    }

    @Test
    public void turnsOnCpuTimeMeasurementWhenItIsOff() {
        AtomicBoolean turnedOn = new AtomicBoolean();
        ThreadMXBean measurementOff = new ThreadBeanStub()
                .answer("isThreadCpuTimeEnabled", args -> turnedOn.get())
                .answer("setThreadCpuTimeEnabled", args -> {
                    turnedOn.set((Boolean) args[0]);
                    return null;
                })
                .bean();

        ThreadSnapshot snapshot = ThreadCpuReader.read(measurementOff);

        assertThat(turnedOn).isTrue();
        assertThat(snapshot.rows()).isNotEmpty();
    }

    @Test
    public void saysWhenTheTargetCannotMeasureCpuTime() {
        ThreadMXBean unsupported = new ThreadBeanStub()
                .answer("isThreadCpuTimeSupported", args -> false)
                .bean();

        ThreadSnapshot snapshot = ThreadCpuReader.read(unsupported);

        assertThat(snapshot.rows()).isEmpty();
        assertThat(snapshot.status()).isEqualTo(ThreadCpuReader.CPU_TIME_UNSUPPORTED);
    }

    @Test
    public void saysWhenTheTargetRefusesToTurnOnCpuTime() {
        ThreadMXBean refusing = new ThreadBeanStub()
                .answer("isThreadCpuTimeEnabled", args -> false)
                .answer("setThreadCpuTimeEnabled", args -> {
                    throw new SecurityException("not allowed");
                })
                .bean();

        ThreadSnapshot snapshot = ThreadCpuReader.read(refusing);

        assertThat(snapshot.rows()).isEmpty();
        assertThat(snapshot.status()).isEqualTo(ThreadCpuReader.CPU_TIME_REFUSED);
    }

    @Test
    public void saysWhenTheTargetHasNoThreadBean() {
        ThreadSnapshot snapshot = ThreadCpuReader.read(null);

        assertThat(snapshot.rows()).isEmpty();
        assertThat(snapshot.status()).isEqualTo(ThreadCpuReader.NO_THREAD_BEAN);
    }

    @Test
    public void equalCpuTimesGoByName() {
        ThreadRow first = new ThreadRow("a", 1.0, Thread.State.RUNNABLE);
        ThreadRow second = new ThreadRow("b", 1.0, Thread.State.WAITING);
        ThreadRow busiest = new ThreadRow("c", 2.0, Thread.State.RUNNABLE);
        List<ThreadRow> rows = new ArrayList<>(List.of(second, first, busiest));

        rows.sort(ThreadRow.BUSIEST_FIRST);

        assertThat(rows).containsExactly(busiest, first, second);
    }

    /** Spins until this thread has used a clear amount of CPU time. */
    private static void burnCpu() {
        ThreadMXBean threads = ManagementFactory.getThreadMXBean();
        while (threads.getCurrentThreadCpuTime() < BUSY_CPU_NANOS) {
            Thread.onSpinWait();
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
