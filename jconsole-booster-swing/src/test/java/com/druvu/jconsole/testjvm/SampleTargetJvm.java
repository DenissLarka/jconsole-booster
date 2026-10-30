package com.druvu.jconsole.testjvm;

import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
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
 * <p>Usage: {@code java ... com.druvu.jconsole.testjvm.SampleTargetJvm [port]} (default port 7091). Connect with
 * {@code mvn exec:exec@start} and target {@code localhost:7091}.
 */
public final class SampleTargetJvm {

    public static final int DEFAULT_PORT = 7091;
    public static final String OBJECT_NAME = "com.druvu.testjvm:type=SampleControl";
    public static final String MARKUP_OBJECT_NAME = "com.druvu.testjvm:type=SampleMarkup";
    public static final String DOWNLOADS_OBJECT_NAME = "com.druvu.testjvm:type=SampleDownloads";

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;

        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

        SampleControl control = new SampleControl();
        mbs.registerMBean(control, new ObjectName(OBJECT_NAME));

        SampleMarkupStandardMBean markup = new SampleMarkupStandardMBean(new SampleMarkup());
        mbs.registerMBean(markup, new ObjectName(MARKUP_OBJECT_NAME));

        SampleDownloadsStandardMBean downloads = new SampleDownloadsStandardMBean(new SampleDownloads());
        mbs.registerMBean(downloads, new ObjectName(DOWNLOADS_OBJECT_NAME));

        // Integration-test target: exposes a secured JMXMP endpoint. Connect from JCB with
        // user/password admin/admin.
        //
        // druvu-lib-jmxmp 2.0.0 makes this zero-config: with no "jmx.remote.profiles" the server
        // defaults to "TLS SASL/PLAIN", and with no "jmx.remote.tls.socket.factory" it generates an
        // ephemeral self-signed certificate on the fly. So a minimal secure target is just a
        // JMXAuthenticator — no keystore, no SSLContext.
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

        JMXServiceURL url = new JMXServiceURL("service:jmx:jmxmp://0.0.0.0:" + port);
        JMXConnectorServer connector = JMXConnectorServerFactory.newJMXConnectorServer(url, env, mbs);
        connector.start();

        System.out.println("Sample target JVM ready.");
        System.out.println("  PID:        " + ProcessHandle.current().pid());
        System.out.println("  JMX URL:    " + url);
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
