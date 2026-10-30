package com.druvu.jconsole.fx.tab.threads;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import org.testng.annotations.Test;

/** A thread's details in words, from real threads of this JVM: the header by lock state, the counts, the stack. */
public class StackTextTest {

    @Test
    public void aRunningThreadHasNameAndStateOnly() {
        Thread current = Thread.currentThread();
        ThreadInfo info = ManagementFactory.getThreadMXBean().getThreadInfo(current.threadId(), Integer.MAX_VALUE);

        String text = StackText.of(info);

        assertThat(text)
                .startsWith("Name: " + current.getName() + "\nState: RUNNABLE\n")
                .contains("Total blocked: ")
                .contains("Total waited: ")
                .contains("\nStack trace: \n")
                .contains(StackTextTest.class.getName() + ".aRunningThreadHasNameAndStateOnly");
    }

    @Test
    public void aWaitingThreadNamesTheLockItWaitsForAndTheMonitorsItHolds() throws InterruptedException {
        try (MonitorHolder monitor = new MonitorHolder("stack-text-waiting")) {
            String text = StackText.of(withLockedMonitors(monitor.holder()));

            assertThat(text)
                    .startsWith("Name: stack-text-waiting-holder\nState: WAITING on ")
                    .doesNotContain("owned by:")
                    .contains("   - locked java.lang.Object@");
        }
    }

    @Test
    public void aBlockedThreadNamesTheLockOwner() throws InterruptedException {
        try (MonitorHolder monitor = new MonitorHolder("stack-text-blocked")) {
            String text = StackText.of(withLockedMonitors(monitor.blocked()));

            assertThat(text)
                    .startsWith("Name: stack-text-blocked-blocked\nState: BLOCKED on java.lang.Object@")
                    .contains(" owned by: stack-text-blocked-holder\n");
        }
    }

    private static ThreadInfo withLockedMonitors(Thread thread) {
        return ManagementFactory.getThreadMXBean().getThreadInfo(new long[] {thread.threadId()}, true, false)[0];
    }
}
