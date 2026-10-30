package com.druvu.jconsole.fx.tab.threads;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import org.testng.annotations.Test;

/** Reading one thread's details from this JVM, with and without the locked monitors, and for a thread that ended. */
public class StackReaderTest {

    @Test
    public void withLockUsageTheStackShowsTheLockedMonitors() throws IOException, InterruptedException {
        StackReader reader = new StackReader(new LocalJvmConnection());

        try (MonitorHolder monitor = new MonitorHolder("stack-reader-locks")) {
            String text = reader.read(monitor.holder().threadId());

            assertThat(text).startsWith("Name: stack-reader-locks-holder\n").contains("   - locked ");
        }
    }

    @Test
    public void withoutLockUsageTheStackHasNoLockedMonitors() throws IOException, InterruptedException {
        StackReader reader = new StackReader(new LocalJvmConnection(ManagementFactory.getThreadMXBean(), false));

        try (MonitorHolder monitor = new MonitorHolder("stack-reader-plain")) {
            String text = reader.read(monitor.holder().threadId());

            assertThat(text)
                    .startsWith("Name: stack-reader-plain-holder\n")
                    .contains(MonitorHolder.class.getName())
                    .doesNotContain("- locked");
        }
    }

    @Test
    public void aThreadThatEndedSaysSo() throws IOException {
        StackReader reader = new StackReader(new LocalJvmConnection());

        assertThat(reader.read(Long.MAX_VALUE)).isEqualTo(StackReader.THREAD_ENDED);
    }
}
