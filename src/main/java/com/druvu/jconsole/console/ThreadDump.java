package com.druvu.jconsole.console;

import java.io.IOException;
import java.lang.management.LockInfo;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.management.JMException;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;

/**
 * Captures a full thread dump from a live JMX connection, as text.
 *
 * <p>Two sources, tried in order — the console's {@code threads} command does not go through
 * {@code ConsoleMain#isCallable}, so the good one is reachable here even though it is not callable via
 * {@code call}/{@code invoke}:
 *
 * <ol>
 *   <li><b>{@code com.sun.management:type=DiagnosticCommand} → {@code threadPrint}</b> — the jcmd {@code Thread.print}
 *       bridge. Returns a genuine, byte-for-byte {@code jstack} dump (deadlock analysis included) that any existing
 *       thread-dump analyzer eats. HotSpot only, and its {@code String[]} parameter is exactly what makes it
 *       un-invokable from the generic {@code call} path ({@code Utils.isEditableType} rejects array types).
 *   <li><b>{@code java.lang:type=Threading} → {@code dumpAllThreads}</b> — the portable fallback for a non-HotSpot
 *       target or one where the diagnostic MBean is absent. The raw result is a {@code CompositeData[]};
 *       {@link #format} rebuilds it into jstack-shaped text here rather than letting {@code ConsoleRenderer} flatten it
 *       to a single {@code Arrays.deepToString} line. Deadlocked ids are appended separately, since only the jcmd path
 *       reports them on its own.
 * </ol>
 *
 * <p>The frame walk is deliberately not {@link ThreadInfo#toString()}: that caps the stack at 8 frames, which throws
 * away the part of a production dump you actually opened it for.
 */
final class ThreadDump {

    /** jcmd's {@code Thread.print} bridge — HotSpot only. */
    private static final ObjectName DIAGNOSTIC_COMMAND = objectName("com.sun.management:type=DiagnosticCommand");

    /** The portable platform MXBean. */
    private static final ObjectName THREADING = objectName("java.lang:type=Threading");

    /** {@code jstack -l}: include the ownable-synchronizer ("Locked ownable synchronizers") section. */
    private static final Object[] THREAD_PRINT_ARGS = {new String[] {"-l"}};

    private static final String[] THREAD_PRINT_SIG = {String[].class.getName()};

    private static final DateTimeFormatter HEADER_STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter FILE_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    /** Keeps a default file name sane when the URL is a long non-jmxmp {@code service:jmx:} form. */
    private static final int MAX_NAME_PART = 40;

    private ThreadDump() {}

    /** A captured dump plus the MBean route that produced it, for the confirmation line. */
    record Dump(String text, String source) {}

    /**
     * Captures a dump over {@code conn}.
     *
     * @return the dump, or {@code null} if the target exposes neither thread MBean (the caller reports that)
     * @throws IOException the connection is gone — propagates to the REPL's "connection lost" handler
     */
    static Dump capture(MBeanServerConnection conn) throws IOException {
        String jstack = threadPrint(conn);
        if (jstack != null) {
            return new Dump(jstack, "DiagnosticCommand.threadPrint (jcmd Thread.print -l)");
        }
        CompositeData[] rows = dumpAllThreads(conn);
        if (rows == null) {
            return null;
        }
        return new Dump(format(rows, findDeadlockedThreads(conn), LocalDateTime.now()), "ThreadMXBean.dumpAllThreads");
    }

    /** Default file name for a dump of {@code target}, e.g. {@code threaddump-localhost-7091-20260824-133000.txt}. */
    static String defaultFileName(String target, LocalDateTime now) {
        String safe = target == null ? "" : target.replaceAll("[^A-Za-z0-9._]+", "-");
        safe = safe.replaceAll("^-+|-+$", "");
        if (safe.length() > MAX_NAME_PART) {
            safe = safe.substring(0, MAX_NAME_PART);
        }
        if (safe.isBlank()) {
            safe = "target";
        }
        return "threaddump-" + safe + "-" + FILE_STAMP.format(now) + ".txt";
    }

    // ----- sources -----

    /** @return the jstack text, or {@code null} when the MBean is absent, non-HotSpot, or refuses the call */
    private static String threadPrint(MBeanServerConnection conn) throws IOException {
        if (DIAGNOSTIC_COMMAND == null || !conn.isRegistered(DIAGNOSTIC_COMMAND)) {
            return null;
        }
        try {
            Object result = conn.invoke(DIAGNOSTIC_COMMAND, "threadPrint", THREAD_PRINT_ARGS, THREAD_PRINT_SIG);
            return (result instanceof String s && !s.isBlank()) ? s : null;
        } catch (JMException | RuntimeException e) {
            return null; // registered but unusable — fall through to the portable route
        }
    }

    private static CompositeData[] dumpAllThreads(MBeanServerConnection conn) throws IOException {
        if (THREADING == null || !conn.isRegistered(THREADING)) {
            return null;
        }
        try {
            Object result = conn.invoke(
                    THREADING,
                    "dumpAllThreads",
                    new Object[] {Boolean.TRUE, Boolean.TRUE}, // lockedMonitors, lockedSynchronizers
                    new String[] {"boolean", "boolean"});
            return (result instanceof CompositeData[] rows) ? rows : null;
        } catch (JMException | RuntimeException e) {
            return null;
        }
    }

    /** Best-effort: a dump without the deadlock section still beats no dump. */
    private static long[] findDeadlockedThreads(MBeanServerConnection conn) throws IOException {
        try {
            Object result = conn.invoke(THREADING, "findDeadlockedThreads", new Object[0], new String[0]);
            return (result instanceof long[] ids) ? ids : null;
        } catch (JMException | RuntimeException e) {
            return null;
        }
    }

    // ----- fallback formatting -----

    /** Rebuilds {@code dumpAllThreads} rows into jstack-shaped text. Package-visible + static for unit testing. */
    static String format(CompositeData[] rows, long[] deadlockedIds, LocalDateTime now) {
        StringBuilder sb = new StringBuilder();
        sb.append(HEADER_STAMP.format(now)).append('\n');
        sb.append("Full thread dump — assembled by JConsoleBooster from ThreadMXBean.dumpAllThreads\n");
        sb.append("(the target exposes no com.sun.management:type=DiagnosticCommand MBean)\n");
        for (CompositeData row : rows) {
            sb.append('\n');
            appendThread(sb, ThreadInfo.from(row));
        }
        appendDeadlocks(sb, deadlockedIds);
        return sb.toString();
    }

    private static void appendThread(StringBuilder sb, ThreadInfo t) {
        sb.append('"').append(t.getThreadName()).append('"').append(" #").append(t.getThreadId());
        if (t.isDaemon()) {
            sb.append(" daemon");
        }
        sb.append(" prio=").append(t.getPriority()).append('\n');
        sb.append("   java.lang.Thread.State: ").append(t.getThreadState()).append('\n');

        StackTraceElement[] stack = t.getStackTrace();
        MonitorInfo[] monitors = t.getLockedMonitors();
        for (int i = 0; i < stack.length; i++) {
            sb.append("\tat ").append(stack[i]).append('\n');
            if (i == 0) {
                appendBlockedOn(sb, t); // jstack prints the contended lock right under the top frame
            }
            for (MonitorInfo m : monitors) {
                if (m.getLockedStackDepth() == i) {
                    sb.append("\t- locked ").append(describe(m)).append('\n');
                }
            }
        }
        if (stack.length == 0) {
            appendBlockedOn(sb, t); // no frames (e.g. a native/unstarted thread) — the lock still matters
        }

        sb.append("\n   Locked ownable synchronizers:\n");
        LockInfo[] synchronizers = t.getLockedSynchronizers();
        if (synchronizers.length == 0) {
            sb.append("\t- None\n");
        } else {
            for (LockInfo l : synchronizers) {
                sb.append("\t- ").append(describe(l)).append('\n');
            }
        }
    }

    private static void appendBlockedOn(StringBuilder sb, ThreadInfo t) {
        LockInfo lock = t.getLockInfo();
        if (lock == null) {
            return;
        }
        String verb = t.getThreadState() == Thread.State.BLOCKED ? "waiting to lock" : "waiting on";
        sb.append("\t- ").append(verb).append(' ').append(describe(lock));
        if (t.getLockOwnerName() != null) {
            sb.append(" owned by \"")
                    .append(t.getLockOwnerName())
                    .append("\" #")
                    .append(t.getLockOwnerId());
        }
        sb.append('\n');
    }

    private static void appendDeadlocks(StringBuilder sb, long[] deadlockedIds) {
        if (deadlockedIds == null || deadlockedIds.length == 0) {
            return;
        }
        sb.append("\nFound ").append(deadlockedIds.length).append(" deadlocked thread(s), by id:");
        for (long id : deadlockedIds) {
            sb.append(' ').append(id);
        }
        sb.append('\n');
    }

    private static String describe(LockInfo lock) {
        return "<0x" + Integer.toHexString(lock.getIdentityHashCode()) + "> (a " + lock.getClassName() + ")";
    }

    /** Constant object names — malformed is impossible for these literals, so a null here would be a coding error. */
    private static ObjectName objectName(String name) {
        try {
            return ObjectName.getInstance(name);
        } catch (MalformedObjectNameException e) {
            return null;
        }
    }
}
