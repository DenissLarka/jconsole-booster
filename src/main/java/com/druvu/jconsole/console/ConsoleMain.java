package com.druvu.jconsole.console;

import com.druvu.jconsole.inspector.Utils;
import com.druvu.jconsole.jmx.JMXConnectionManager;
import com.druvu.jconsole.jmx.JMXConnectionManager.ConnectionResult;
import com.druvu.jconsole.launcher.ArgumentParser;
import com.druvu.jconsole.launcher.JConsoleOptions;
import com.druvu.jconsole.util.RecentConnections;
import com.druvu.jconsole.util.Version;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.remote.JMXServiceURL;

/**
 * Interactive command-line ({@code --console}) mode: a text REPL over the existing adaptive-TLS / trust-on-first-use /
 * credential-hard-block connection stack ({@link JMXConnectionManager}). Fully headless-capable — the console path
 * never references {@code JConsole} (a {@code JFrame} subclass) and never needs a display.
 *
 * <p>Scope (user steer, 2026-07-18): MBean <em>operations</em>. The core is a numbered drill-down — {@code beans} →
 * pick a bean → its operations → pick one → prompted args → {@code call} → rendered result. A one-shot {@code invoke
 * <objectName> <op> [args…]} is the self-contained form (handy for {@code -e} script mode). Attribute read/write,
 * domain browsing, bookmarks and notifications are out of v1.
 *
 * <p>With one or more {@code -e=<cmd>} flags the mode is non-interactive (see {@link #runScript}): auto-open the
 * target, run the commands in order, exit 0 on success / 1 on the first failure. The REPL is driven through a
 * {@link ConsoleIO} injected into the package-visible constructor, so tests script input, capture output, and read the
 * exit code without {@code System.setIn}/{@code System.exit} games.
 */
public final class ConsoleMain {

    private final ConsoleIO io;
    private ConsoleSession session;

    // Drill-down selection state.
    private List<ObjectName> lastBeans; // last 'beans' listing, for index selection
    private ObjectName currentBean; // selected via 'bean'
    private List<MBeanOperationInfo> currentOps; // current bean's operations, for index selection

    // Set by fail(); consulted by runScript() for the process exit code (ignored in interactive mode).
    private boolean failed;

    ConsoleMain(ConsoleIO io) {
        this.io = io;
    }

    /**
     * Production entry: forces headless, wires stdin/stdout, installs the console prompts, runs interactive or script.
     */
    public static void run(JConsoleOptions options) {
        // Nothing on the console path needs a display; set this FIRST, defensively, so an accidental
        // toolkit init inside a shared helper cannot pop AWT on a headless box.
        System.setProperty("java.awt.headless", "true");
        ConsoleIO io = new ConsoleIO(
                new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8)), System.out, true);
        ConsoleMain main = new ConsoleMain(io);
        if (options.commands().isEmpty()) {
            main.loop(options);
        } else {
            System.exit(main.runScript(options));
        }
    }

    /** Installs the console-specific TOFU + plaintext prompts (the console analog of JConsole.main:942-943). */
    private void installPrompts() {
        JMXConnectionManager.setCertTrustPrompt(new ConsoleCertTrustPrompt(io));
        JMXConnectionManager.setPlaintextCredentialAlert(new ConsolePlaintextAlert(io));
    }

    /** Runs the REPL until {@code exit}/{@code quit} or EOF. Package-visible so tests can drive it directly. */
    void loop(JConsoleOptions options) {
        installPrompts();
        io.println("JConsoleBooster console — type 'help' for commands, 'quit' to leave.");
        if (!options.urls().isEmpty()) {
            // urls() are already adapted (host:port -> service:jmx:jmxmp://...) by ArgumentParser.
            doOpen(options.urls().get(0), options.scriptUser(), false);
        }
        while (true) {
            io.print(promptString());
            io.out().flush();
            String line;
            try {
                line = io.readLine();
            } catch (IOException e) {
                break;
            }
            if (line == null) {
                break; // EOF (Ctrl-D / end of scripted input)
            }
            line = line.strip();
            if (line.isEmpty()) {
                continue;
            }
            try {
                if (!dispatch(line)) {
                    break;
                }
            } catch (IOException e) {
                // A live-connection command failed mid-flight: the connection is gone. 'open' failures
                // are handled inside doOpen and never reach here.
                io.println("connection lost (" + e.getMessage() + ") — use 'open' to reconnect");
                closeSession();
            }
        }
        closeSession();
        io.println("bye");
    }

    /**
     * Non-interactive script mode: auto-open the (required) target, run each {@code -e} command in order, stop at the
     * first failure. Returns the process exit code (0 = all ok, 1 = connect or command failure). Package-visible and
     * returning the code (rather than calling {@code System.exit}) so tests can assert it.
     */
    int runScript(JConsoleOptions options) {
        installPrompts();
        if (options.urls().isEmpty()) {
            io.println("script mode (-e) requires a connection target");
            return 1;
        }
        doOpen(options.urls().get(0), options.scriptUser(), false);
        if (session == null) {
            return 1; // connect failed — doOpen already explained
        }
        for (String cmd : options.commands()) {
            try {
                dispatch(cmd);
            } catch (IOException e) {
                io.println("connection lost (" + e.getMessage() + ")");
                closeSession();
                return 1;
            }
            if (failed) {
                break;
            }
        }
        closeSession();
        return failed ? 1 : 0;
    }

    private String promptString() {
        return currentBean == null ? "jcb> " : "jcb " + simpleName(currentBean) + "> ";
    }

    /**
     * Dispatches one command line. Declared to throw {@link IOException}: live-connection commands (beans/bean/call/
     * invoke) let a connection failure propagate to the caller's "connection lost" handler.
     *
     * @return {@code false} to end the REPL
     */
    private boolean dispatch(String line) throws IOException {
        List<String> tokens = tokenize(line);
        if (tokens.isEmpty()) {
            return true;
        }
        String[] parts = tokens.toArray(new String[0]);
        String cmd = parts[0].toLowerCase();
        switch (cmd) {
            case "help", "?" -> printHelp();
            case "version" -> Version.print(io.out());
            case "open" -> cmdOpen(parts);
            case "close" -> cmdClose();
            case "beans" -> cmdBeans(parts);
            case "bean" -> cmdBean(parts);
            case "ops" -> cmdOps();
            case "call" -> cmdCall(parts);
            case "invoke" -> cmdInvoke(parts);
            case "exit", "quit" -> {
                return false;
            }
            default -> fail("unknown command: " + cmd + " (try 'help')");
        }
        return true;
    }

    // ----- connection commands -----

    private void cmdOpen(String[] parts) {
        boolean strict = false;
        int i = 1;
        if (i < parts.length && parts[i].equals("--strict")) {
            strict = true;
            i++;
        }
        if (i >= parts.length) {
            fail("usage: open [--strict] <host:port | service:jmx: url> [user]");
            return;
        }
        String target = parts[i++];
        String user = (i < parts.length) ? parts[i] : null;
        doOpen(ArgumentParser.adaptUrl(target), user, strict);
    }

    /** Opens a connection to an already-adapted service URL, replacing any current session. */
    private void doOpen(String url, String user, boolean strict) {
        if (session != null) {
            io.println("closing existing connection to " + ArgumentParser.shortenUrl(session.url()));
            closeSession();
        }
        String password = null;
        if (user != null) {
            try {
                password = io.readPassword("Password for " + user + ": ");
            } catch (IOException e) {
                fail("could not read password: " + e.getMessage());
                return;
            }
        }
        try {
            JMXServiceURL jmxUrl = new JMXServiceURL(url);
            ConnectionResult result = JMXConnectionManager.connect(jmxUrl, user, password, strict);
            session = new ConsoleSession(url, result);
            RecentConnections.record(url); // GUI records MRU in VMPanel's listener; console does its own
            io.println("connected to " + ArgumentParser.shortenUrl(url)
                    + (result.hasPlatformMXBeans() ? " (platform MXBeans present)" : ""));
        } catch (IOException e) {
            // Includes an operator-declined certificate and the plaintext-credential hard refusal (whose
            // alert has already explained the block). Report as a connect failure; session stays null.
            fail("connect failed: " + e.getMessage());
        }
    }

    private void cmdClose() {
        if (notConnected()) {
            return;
        }
        String shown = ArgumentParser.shortenUrl(session.url());
        closeSession();
        io.println("closed " + shown);
    }

    private void closeSession() {
        if (session != null) {
            session.close();
            session = null;
        }
        lastBeans = null;
        currentBean = null;
        currentOps = null;
    }

    // ----- MBean drill-down commands -----

    private void cmdBeans(String[] parts) throws IOException {
        if (notConnected()) {
            return;
        }
        String filter = parts.length > 1 ? beanFilter(parts[1]) : null;
        Set<ObjectName> names = session.connection().queryNames(null, null);
        List<ObjectName> sorted = new ArrayList<>(names);
        sorted.sort(Comparator.comparing(ObjectName::getCanonicalName));
        if (filter != null) {
            sorted.removeIf(on -> !on.getCanonicalName().toLowerCase().contains(filter));
        }
        lastBeans = sorted;
        if (sorted.isEmpty()) {
            io.println("no MBeans" + (filter != null ? " matching '" + filter + "'" : ""));
            return;
        }
        for (int i = 0; i < sorted.size(); i++) {
            io.println("[" + i + "] " + sorted.get(i).getCanonicalName());
        }
    }

    private void cmdBean(String[] parts) throws IOException {
        if (notConnected()) {
            return;
        }
        if (parts.length < 2) {
            fail("usage: bean <n | objectName>   (n = index from 'beans')");
            return;
        }
        ObjectName target = resolveBean(parts[1]);
        if (target == null) {
            return; // fail() already called
        }
        MBeanInfo info = mbeanInfo(target);
        if (info == null) {
            return;
        }
        currentBean = target;
        currentOps = List.of(info.getOperations());
        printOps();
    }

    private void cmdOps() {
        if (notConnected() || noBean()) {
            return;
        }
        printOps();
    }

    private void cmdCall(String[] parts) throws IOException {
        if (notConnected() || noBean()) {
            return;
        }
        if (parts.length < 2) {
            fail("usage: call <n | opName> [args…]   (prompts for each argument if omitted)");
            return;
        }
        executeCall(currentBean, currentOps, parts[1], parts, 2);
    }

    /** One-shot, self-contained form: {@code invoke <objectName> <operation> [args…]} — no prior 'bean' needed. */
    private void cmdInvoke(String[] parts) throws IOException {
        if (notConnected()) {
            return;
        }
        if (parts.length < 3) {
            fail("usage: invoke <objectName> <operation> [args…]");
            return;
        }
        ObjectName target = resolveBean(parts[1]);
        if (target == null) {
            return;
        }
        MBeanInfo info = mbeanInfo(target);
        if (info == null) {
            return;
        }
        executeCall(target, List.of(info.getOperations()), parts[2], parts, 3);
    }

    /** Resolve the op on {@code ops}, gather args from {@code parts[argStart..]}, coerce, invoke on {@code bean}. */
    private void executeCall(
            ObjectName bean, List<MBeanOperationInfo> ops, String opSelector, String[] parts, int argStart)
            throws IOException {
        OpResolution res = resolveOp(ops, opSelector, parts.length - argStart);
        if (res.op() == null) {
            fail(res.error());
            return;
        }
        MBeanOperationInfo op = res.op();
        if (!isCallable(op)) {
            fail("operation '" + op.getName() + "' has a parameter type that cannot be built from text — not callable");
            return;
        }
        MBeanParameterInfo[] sig = op.getSignature();
        String[] rawArgs = collectArgs(parts, argStart, sig);
        if (rawArgs == null) {
            return; // count/abort message already emitted via fail()
        }
        Object[] params = new Object[sig.length];
        String[] types = new String[sig.length];
        for (int i = 0; i < sig.length; i++) {
            types[i] = sig[i].getType();
            try {
                params[i] = Utils.createObjectFromString(sig[i].getType(), rawArgs[i]);
            } catch (Exception e) {
                fail("could not convert argument " + (i + 1) + " ('" + rawArgs[i] + "') to " + sig[i].getType() + ": "
                        + e.getMessage());
                return;
            }
        }
        invokeAndRender(bean, op, params, types);
    }

    /** Invokes on the RAW connection; IOException propagates (connection lost), operation errors are unwrapped. */
    private void invokeAndRender(ObjectName bean, MBeanOperationInfo op, Object[] params, String[] types)
            throws IOException {
        Object result;
        try {
            result = session.connection().invoke(bean, op.getName(), params, types);
        } catch (MBeanException | ReflectionException | InstanceNotFoundException | RuntimeException e) {
            Throwable actual = Utils.getActualException(e);
            fail("invocation failed: " + actual.getClass().getSimpleName()
                    + (actual.getMessage() != null ? ": " + actual.getMessage() : ""));
            return;
        }
        String ret = op.getReturnType();
        if (ret.equals("void") || ret.equals(Void.TYPE.getName()) || ret.equals(Void.class.getName())) {
            io.println("OK");
            return;
        }
        String rendered = ConsoleRenderer.render(result);
        if (rendered.contains("\n")) {
            io.println("=>");
            io.println(rendered);
        } else {
            io.println("=> " + rendered);
        }
    }

    // ----- helpers -----

    private MBeanInfo mbeanInfo(ObjectName target) throws IOException {
        try {
            return session.connection().getMBeanInfo(target);
        } catch (InstanceNotFoundException | IntrospectionException | ReflectionException e) {
            fail("cannot read MBeanInfo for " + target.getCanonicalName() + ": " + e.getMessage());
            return null;
        }
    }

    private boolean notConnected() {
        if (session == null) {
            fail("not connected — use 'open <host:port> [user]' first");
            return true;
        }
        return false;
    }

    private boolean noBean() {
        if (currentBean == null) {
            fail("no bean selected — use 'bean <n | objectName>' first");
            return true;
        }
        return false;
    }

    private ObjectName resolveBean(String arg) {
        Integer idx = tryParseIndex(arg);
        if (idx != null) {
            if (lastBeans == null || idx < 0 || idx >= lastBeans.size()) {
                fail("no bean [" + idx + "] — run 'beans' first");
                return null;
            }
            return lastBeans.get(idx);
        }
        try {
            return ObjectName.getInstance(arg);
        } catch (Exception e) {
            fail("not an index or a valid ObjectName: " + arg);
            return null;
        }
    }

    private void printOps() {
        io.println(simpleName(currentBean) + " — operations:");
        if (currentOps.isEmpty()) {
            io.println("  (none)");
            return;
        }
        for (int i = 0; i < currentOps.size(); i++) {
            MBeanOperationInfo op = currentOps.get(i);
            String suffix = isCallable(op) ? "" : "   [not callable]";
            io.println("[" + i + "] " + signature(op) + suffix);
        }
    }

    /**
     * Prompts for each argument when none were supplied inline; uses inline args when the full set was given; refuses a
     * partial/wrong count. Returns raw string values, or {@code null} if reading was aborted or the count was wrong (in
     * which case {@link #fail} has been called).
     */
    private String[] collectArgs(String[] parts, int argStart, MBeanParameterInfo[] sig) {
        int inline = parts.length - argStart;
        String[] args = new String[sig.length];
        if (inline == sig.length) {
            for (int i = 0; i < sig.length; i++) {
                args[i] = parts[argStart + i];
            }
            return args;
        }
        if (inline > 0) {
            fail("operation takes " + sig.length + " argument(s), got " + inline);
            return null;
        }
        for (int i = 0; i < sig.length; i++) {
            MBeanParameterInfo p = sig[i];
            String label = (p.getName() != null && !p.getName().isBlank()) ? p.getName() : "arg" + i;
            io.print("  " + label + " (" + readable(p.getType()) + "): ");
            io.out().flush();
            String v;
            try {
                v = io.readLine();
            } catch (IOException e) {
                fail("input error: " + e.getMessage());
                return null;
            }
            if (v == null) {
                fail("aborted");
                return null;
            }
            args[i] = v;
        }
        return args;
    }

    private void fail(String message) {
        io.println(message);
        failed = true;
    }

    private static boolean isCallable(MBeanOperationInfo op) {
        for (MBeanParameterInfo p : op.getSignature()) {
            if (!Utils.isEditableType(p.getType())) {
                return false;
            }
        }
        return true;
    }

    private static String signature(MBeanOperationInfo op) {
        StringBuilder sb = new StringBuilder(op.getName()).append('(');
        MBeanParameterInfo[] params = op.getSignature();
        for (int i = 0; i < params.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(readable(params[i].getType()));
        }
        return sb.append(") : ").append(readable(op.getReturnType())).toString();
    }

    private static String readable(String type) {
        return type.startsWith("java.lang.") ? type.substring("java.lang.".length()) : type;
    }

    private static String simpleName(ObjectName on) {
        String type = on.getKeyProperty("type");
        if (type != null) {
            return type;
        }
        String name = on.getKeyProperty("name");
        return name != null ? name : on.getCanonicalName();
    }

    private static Integer tryParseIndex(String s) {
        if (s.isEmpty() || !s.chars().allMatch(Character::isDigit)) {
            return null;
        }
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return null; // overflow etc. — treat as a name, not an index
        }
    }

    /**
     * Splits a command line into tokens on whitespace, with double-quoted spans kept as single tokens (quotes stripped;
     * {@code ""} yields an empty-string token). No new dependency — the tokenizer the inline {@code call} args reuse.
     * Package-visible + static for unit testing.
     */
    static List<String> tokenize(String line) {
        List<String> tokens = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        boolean have = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    inQuotes = false;
                } else {
                    cur.append(c);
                }
            } else if (c == '"') {
                inQuotes = true;
                have = true; // a quoted span is a token even if empty
            } else if (Character.isWhitespace(c)) {
                if (have) {
                    tokens.add(cur.toString());
                    cur.setLength(0);
                    have = false;
                }
            } else {
                cur.append(c);
                have = true;
            }
        }
        if (have) {
            tokens.add(cur.toString());
        }
        return tokens;
    }

    /**
     * Normalizes the {@code beans} filter argument. The help's {@code beans [filter]} notation invites literal
     * {@code beans filter=word} and {@code beans [word]} spellings — accept both, plus the bare word. The match itself
     * is a case-insensitive substring over the canonical name, domain included. Returns {@code null} when nothing
     * usable remains (blank = unfiltered). Package-visible + static for unit testing.
     */
    static String beanFilter(String raw) {
        String f = raw;
        if (f.regionMatches(true, 0, "filter=", 0, 7)) {
            f = f.substring(7);
        }
        if (f.length() >= 2 && f.startsWith("[") && f.endsWith("]")) {
            f = f.substring(1, f.length() - 1);
        }
        return f.isBlank() ? null : f.toLowerCase();
    }

    /** Resolves an operation by list index or by name (+arity to disambiguate overloads). Static + testable. */
    static OpResolution resolveOp(List<MBeanOperationInfo> ops, String sel, int suppliedArgCount) {
        Integer idx = tryParseIndex(sel);
        if (idx != null) {
            if (idx < 0 || idx >= ops.size()) {
                return OpResolution.err("no operation [" + idx + "] — run 'ops'");
            }
            return OpResolution.ok(ops.get(idx));
        }
        List<MBeanOperationInfo> byName = new ArrayList<>();
        for (MBeanOperationInfo op : ops) {
            if (op.getName().equals(sel)) {
                byName.add(op);
            }
        }
        if (byName.isEmpty()) {
            return OpResolution.err("no operation named '" + sel + "' (try 'ops')");
        }
        if (byName.size() == 1) {
            return OpResolution.ok(byName.get(0));
        }
        List<MBeanOperationInfo> byArity = new ArrayList<>();
        for (MBeanOperationInfo op : byName) {
            if (op.getSignature().length == suppliedArgCount) {
                byArity.add(op);
            }
        }
        if (byArity.size() == 1) {
            return OpResolution.ok(byArity.get(0));
        }
        StringBuilder msg = new StringBuilder("'" + sel + "' is overloaded — pick by index:");
        for (int i = 0; i < ops.size(); i++) {
            if (ops.get(i).getName().equals(sel)) {
                msg.append("\n  [").append(i).append("] ").append(signature(ops.get(i)));
            }
        }
        return OpResolution.err(msg.toString());
    }

    /** Outcome of {@link #resolveOp}: exactly one of {@code op} / {@code error} is non-null. */
    record OpResolution(MBeanOperationInfo op, String error) {
        static OpResolution ok(MBeanOperationInfo op) {
            return new OpResolution(op, null);
        }

        static OpResolution err(String message) {
            return new OpResolution(null, message);
        }
    }

    private void printHelp() {
        io.println("commands:");
        io.println("  help                              show this help");
        io.println("  version                           print version");
        io.println("  open [--strict] <target> [user]   connect (host:port or service:jmx: url;");
        io.println("                                    --strict rejects untrusted TLS certs)");
        io.println("  close                             close the current connection");
        io.println("  beans [filter]                    list MBeans, numbered (filter: case-insensitive");
        io.println("                                    substring of the full name, package included)");
        io.println("  bean <n | objectName>             select a bean and list its operations");
        io.println("  ops                               re-list the current bean's operations");
        io.println("  call <n | opName> [args…]         invoke on the selected bean (prompts for args if omitted)");
        io.println("  invoke <objectName> <op> [args…]  one-shot invoke without selecting a bean first");
        io.println("  exit | quit                       leave the console");
    }
}
