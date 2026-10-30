package com.druvu.jconsole.fx.tab.threads;

import com.druvu.jconsole.core.text.Messages;
import com.druvu.jconsole.core.text.Resources;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;

/**
 * A thread's details in words, as the JDK's JConsole writes them: the name, the state and the lock waited for, the
 * blocked and waited counts, and the stack with the monitors each frame holds.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
final class StackText {

    private StackText() {}

    /** The details of {@code info}; the "- locked" lines appear when it was read with its locked monitors. */
    static String of(ThreadInfo info) {
        StringBuilder text = new StringBuilder(header(info));
        text.append(
                Resources.format(Messages.BLOCKED_COUNT_WAITED_COUNT, info.getBlockedCount(), info.getWaitedCount()));
        text.append(Messages.STACK_TRACE);
        StackTraceElement[] frames = info.getStackTrace();
        MonitorInfo[] monitors = info.getLockedMonitors();
        for (int depth = 0; depth < frames.length; depth++) {
            text.append(frames[depth]).append('\n');
            for (MonitorInfo monitor : monitors) {
                if (monitor.getLockedStackDepth() == depth) {
                    text.append(Resources.format(Messages.MONITOR_LOCKED, monitor.toString()));
                }
            }
        }
        return text.toString();
    }

    /** The name and state, with the lock the thread waits for and its owner when there are. */
    private static String header(ThreadInfo info) {
        String name = info.getThreadName();
        String state = info.getThreadState().toString();
        if (info.getLockName() == null) {
            return Resources.format(Messages.NAME_STATE, name, state);
        }
        if (info.getLockOwnerName() == null) {
            return Resources.format(Messages.NAME_STATE_LOCK_NAME, name, state, info.getLockName());
        }
        return Resources.format(
                Messages.NAME_STATE_LOCK_NAME_LOCK_OWNER, name, state, info.getLockName(), info.getLockOwnerName());
    }
}
