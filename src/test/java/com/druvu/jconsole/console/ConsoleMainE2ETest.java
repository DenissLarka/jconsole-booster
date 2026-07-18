package com.druvu.jconsole.console;

import com.druvu.jconsole.launcher.ArgumentParser;
import com.druvu.jconsole.launcher.JConsoleOptions;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import javax.management.remote.JMXAuthenticator;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXPrincipal;
import javax.management.remote.JMXServiceURL;
import javax.security.auth.Subject;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Headless end-to-end for {@link ConsoleMain}, driven through an injected {@link ConsoleIO} against a live TLS jmxmp
 * target (admin/admin, ephemeral self-signed cert — the druvu-lib-jmxmp 2.0.0 default). Exercises the full stack: TOFU
 * trust prompt, connect, the interactive drill-down (beans → bean → call), and non-interactive {@code -e} script mode
 * including exit codes.
 */
public class ConsoleMainE2ETest {

    private static final String OBJ = "com.druvu.test:type=Echo";

    public interface EchoMBean {
        String echo(String s);

        int add(int a, int b);
    }

    public static final class Echo implements EchoMBean {
        @Override
        public String echo(String s) {
            return "echo:" + s;
        }

        @Override
        public int add(int a, int b) {
            return a + b;
        }
    }

    @Test
    public void interactiveDrillDownAndInvoke() throws Exception {
        int port = freePort();
        JMXConnectorServer server = startSecuredTarget(port);
        try {
            String input = String.join(
                            "\n",
                            "open localhost:" + port + " admin",
                            "admin", // password (piped → readPassword falls back to readLine)
                            "o", // trust once
                            "beans Echo",
                            "bean " + OBJ,
                            "call echo hello",
                            "quit")
                    + "\n";
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            JConsoleOptions opts =
                    ArgumentParser.parse(new String[] {"--console"}).orElseThrow();

            new ConsoleMain(io(input, out)).loop(opts);

            String o = out.toString(StandardCharsets.UTF_8);
            Assert.assertTrue(o.contains("Untrusted server certificate"), o); // TOFU prompt fired
            Assert.assertTrue(o.contains("connected to localhost:" + port), o); // connected banner
            Assert.assertTrue(o.contains("echo(String) : String"), o); // operations listed
            Assert.assertTrue(o.contains("=> echo:hello"), o); // invoke result rendered
            Assert.assertTrue(o.contains("bye"), o); // clean exit
        } finally {
            server.stop();
        }
    }

    @Test
    public void scriptModeSucceedsWithExitZero() throws Exception {
        int port = freePort();
        JMXConnectorServer server = startSecuredTarget(port);
        try {
            // stdin supplies only password + trust response; commands come from -e.
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            JConsoleOptions opts = ArgumentParser.parse(
                            new String[] {"-u=admin", "-e=invoke " + OBJ + " add 2 3", "localhost:" + port})
                    .orElseThrow();

            int code = new ConsoleMain(io("admin\no\n", out)).runScript(opts);

            String o = out.toString(StandardCharsets.UTF_8);
            Assert.assertEquals(code, 0, o);
            Assert.assertTrue(o.contains("=> 5"), o); // add(2,3) rendered
        } finally {
            server.stop();
        }
    }

    @Test
    public void scriptModeFailsWithExitOne() throws Exception {
        int port = freePort();
        JMXConnectorServer server = startSecuredTarget(port);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            JConsoleOptions opts = ArgumentParser.parse(
                            new String[] {"-u=admin", "-e=invoke " + OBJ + " noSuchOp", "localhost:" + port})
                    .orElseThrow();

            int code = new ConsoleMain(io("admin\no\n", out)).runScript(opts);

            Assert.assertEquals(code, 1, out.toString(StandardCharsets.UTF_8)); // command failure → exit 1
        } finally {
            server.stop();
        }
    }

    // ----- harness -----

    private static ConsoleIO io(String input, ByteArrayOutputStream out) {
        return new ConsoleIO(
                new BufferedReader(new StringReader(input)), new PrintStream(out, true, StandardCharsets.UTF_8));
    }

    /** A druvu-lib-jmxmp 2.0.0 secured target (TLS SASL/PLAIN, ephemeral self-signed cert) with an Echo MBean. */
    private static JMXConnectorServer startSecuredTarget(int port) throws Exception {
        MBeanServer mbs = MBeanServerFactory.newMBeanServer();
        mbs.registerMBean(new Echo(), new ObjectName(OBJ));
        JMXAuthenticator authenticator = credentials -> {
            if (!(credentials instanceof String[] c)
                    || c.length != 2
                    || !"admin".equals(c[0])
                    || !"admin".equals(c[1])) {
                throw new SecurityException("Invalid credentials (expected admin/admin)");
            }
            return new Subject(true, Set.of(new JMXPrincipal(c[0])), Set.of(), Set.of());
        };
        Map<String, Object> env = new HashMap<>();
        env.put(JMXConnectorServer.AUTHENTICATOR, authenticator);
        JMXServiceURL url = new JMXServiceURL("service:jmx:jmxmp://localhost:" + port);
        JMXConnectorServer server = JMXConnectorServerFactory.newJMXConnectorServer(url, env, mbs);
        server.start();
        return server;
    }

    private static int freePort() throws IOException {
        try (ServerSocket s = new ServerSocket(0)) {
            return s.getLocalPort();
        }
    }
}
