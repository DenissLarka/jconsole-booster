package com.druvu.jconsole.fx.tab.threads;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;
import java.util.stream.IntStream;
import org.testng.annotations.Test;

/** Finding real deadlocks between threads of this JVM and grouping them into cycles, and the tabs' titles. */
public class DeadlockReaderTest {

    @Test
    public void findsNoDeadlockWhenThereIsNone() throws IOException {
        assertThat(DeadlockReader.read(new LocalJvmConnection())).isEmpty();
    }

    @Test
    public void groupsTwoThreadsWaitingOnEachOtherIntoOneCycle() throws IOException, InterruptedException {
        try (DeadlockRing ring = DeadlockRing.start("deadlock-pair", 2)) {
            List<DeadlockCycle> cycles = DeadlockReader.read(new LocalJvmConnection());

            assertThat(cycles).hasSize(1);
            assertThat(names(cycles.getFirst())).containsExactlyInAnyOrderElementsOf(ring.names());
        }
    }

    @Test
    public void groupsTwoDeadlocksIntoTwoCycles() throws IOException, InterruptedException {
        try (DeadlockRing pair = DeadlockRing.start("deadlock-two", 2);
                DeadlockRing triangle = DeadlockRing.start("deadlock-three", 3)) {
            List<DeadlockCycle> cycles = DeadlockReader.read(new LocalJvmConnection());

            assertThat(cycles)
                    .extracting(DeadlockReaderTest::names)
                    .anySatisfy(names -> assertThat(names).containsExactlyInAnyOrderElementsOf(pair.names()))
                    .anySatisfy(names -> assertThat(names).containsExactlyInAnyOrderElementsOf(triangle.names()))
                    .hasSize(2);
        }
    }

    @Test
    public void aCycleFollowsTheLockOwners() throws IOException, InterruptedException {
        try (DeadlockRing ring = DeadlockRing.start("deadlock-ring", 3)) {
            List<String> cycle =
                    names(DeadlockReader.read(new LocalJvmConnection()).getFirst());

            // each thread waits for the lock of the one after it in the ring, so the cycle is the ring turned
            List<String> ringFromCycleStart = rotate(ring.names(), ring.names().indexOf(cycle.getFirst()));
            assertThat(cycle).isEqualTo(ringFromCycleStart);
        }
    }

    @Test
    public void namesTheTabOfALoneDeadlockWithoutANumber() {
        assertThat(DeadlockCycle.tabTitle(0, 1)).isEqualTo("Deadlock");
        assertThat(DeadlockCycle.tabTitle(0, 2)).isEqualTo("Deadlock 1");
        assertThat(DeadlockCycle.tabTitle(1, 2)).isEqualTo("Deadlock 2");
    }

    private static List<String> names(DeadlockCycle cycle) {
        return cycle.threads().stream().map(ThreadEntry::name).toList();
    }

    private static List<String> rotate(List<String> names, int start) {
        return IntStream.range(0, names.size())
                .mapToObj(i -> names.get((start + i) % names.size()))
                .toList();
    }
}
