package com.druvu.jconsole.sampletarget;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.remote.JMXAuthenticator;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXPrincipal;
import javax.management.remote.JMXServiceURL;
import javax.security.auth.Subject;

/**
 * Sample target JVM. Starts a JMXMP connector and a custom MBean, then drives synthetic load (CPU, allocations, named
 * threads) so JConsole has something interesting to display.
 *
 * <p>Usage: {@code java ... com.druvu.jconsole.sampletarget.SampleTargetJvm [port]} (default port 7091), or
 * {@code ./Start-TestJvm.ps1}. Connect with {@code ./Start-JConsole.ps1 localhost:7091}.
 *
 * @author Deniss Larka <br>
 *     on 25 Apr 2026
 */
public final class SampleTargetJvm {

    public static final int DEFAULT_PORT = 7091;
    public static final String OBJECT_NAME = "com.druvu.testjvm:type=SampleControl";
    public static final String MARKUP_OBJECT_NAME = "com.druvu.testjvm:type=SampleMarkup";
    public static final String DOWNLOADS_OBJECT_NAME = "com.druvu.testjvm:type=SampleDownloads";

    // 0.0.0.0 is the wildcard bind address, every interface, so a JConsole Booster on another box can connect too;
    // it names no host, which is what the rule guards against.
    @SuppressWarnings("PMD.AvoidUsingHardCodedIP")
    public static void main(String[] args) throws JMException, IOException, InterruptedException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;

        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        SampleControl control = new SampleControl();
        registerMBeans(mbs, control);
        JMXConnectorServer connector = startSecuredEndpoint(mbs, "0.0.0.0", port);

        System.out.println("Sample target JVM ready.");
        System.out.println("  PID:        " + ProcessHandle.current().pid());
        System.out.println("  JMX URL:    " + connector.getAddress());
        System.out.println("  Connect:    localhost:" + port);
        System.out.println("  MBean:      " + OBJECT_NAME);
        System.out.println("              " + MARKUP_OBJECT_NAME);
        System.out.println("              " + DOWNLOADS_OBJECT_NAME);
        System.out.println("Press Ctrl+C to stop.");

        startCpuChurn(control);
        startAllocationChurn();
        startNamedIdleThreads();

        Runtime.getRuntime()
                .addShutdownHook(new Thread(
                        () -> {
                            try {
                                connector.stop();
                            } catch (Exception ignored) {
                            }
                        },
                        "sample-shutdown"));

        Thread.currentThread().join();
    }

    /** Registers the three sample MBeans on {@code mbs}: the control panel, the markup showcase and the downloads. */
    public static void registerMBeans(MBeanServer mbs, SampleControl control) throws JMException {
        mbs.registerMBean(control, new ObjectName(OBJECT_NAME));
        mbs.registerMBean(new SampleMarkupStandardMBean(new SampleMarkup()), new ObjectName(MARKUP_OBJECT_NAME));
        mbs.registerMBean(
                new SampleDownloadsStandardMBean(new SampleDownloads()), new ObjectName(DOWNLOADS_OBJECT_NAME));
    }

    /**
     * Starts the secured JMXMP endpoint over {@code mbs} on {@code host:port}; connect as admin/admin.
     *
     * <p>druvu-lib-jmxmp makes this zero-config: with no {@code jmx.remote.profiles} the server defaults to {@code TLS
     * SASL/PLAIN}, and with no {@code jmx.remote.tls.socket.factory} it generates an ephemeral self-signed certificate
     * on the fly. So a minimal secure target is just a {@link JMXAuthenticator}: no keystore, no {@code SSLContext}.
     */
    public static JMXConnectorServer startSecuredEndpoint(MBeanServer mbs, String host, int port) throws IOException {
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

        JMXServiceURL url = new JMXServiceURL("service:jmx:jmxmp://" + host + ":" + port);
        JMXConnectorServer connector = JMXConnectorServerFactory.newJMXConnectorServer(url, env, mbs);
        connector.start();
        return connector;
    }

    private static void startCpuChurn(SampleControl control) {
        Thread t = new Thread(
                () -> {
                    Random rnd = new Random();
                    while (!Thread.currentThread().isInterrupted()) {
                        control.recordRequest();
                        long burnUntil =
                                System.nanoTime() + ThreadLocalRandom.current().nextLong(20_000_000L, 80_000_000L);
                        double acc = 0;
                        while (System.nanoTime() < burnUntil) {
                            acc += Math.sqrt(rnd.nextDouble());
                        }
                        if (acc < 0) {
                            System.out.println(acc);
                        }
                        try {
                            Thread.sleep(ThreadLocalRandom.current().nextInt(50, 250));
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }
                },
                "sample-cpu-churn");
        t.setDaemon(true);
        t.start();
    }

    private static void startAllocationChurn() {
        Thread t = new Thread(
                () -> {
                    while (!Thread.currentThread().isInterrupted()) {
                        int size = ThreadLocalRandom.current().nextInt(64, 1024) * 1024;
                        byte[] junk = new byte[size];
                        junk[0] = 1;
                        if (junk[size - 1] == 99) {
                            System.out.println("never");
                        }
                        try {
                            Thread.sleep(ThreadLocalRandom.current().nextInt(20, 120));
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }
                },
                "sample-alloc-churn");
        t.setDaemon(true);
        t.start();
    }

    private static void startNamedIdleThreads() {
        String[] names = {"sample-worker-1", "sample-worker-2", "sample-scheduler", "sample-io-poller"};
        for (String name : names) {
            Thread t = new Thread(
                    () -> {
                        Object lock = new Object();
                        synchronized (lock) {
                            while (!Thread.currentThread().isInterrupted()) {
                                try {
                                    lock.wait(5_000);
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                    return;
                                }
                            }
                        }
                    },
                    name);
            t.setDaemon(true);
            t.start();
        }
    }

    private SampleTargetJvm() {}
}
