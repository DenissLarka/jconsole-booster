package com.druvu.jconsole.testjvm;

import java.lang.management.ManagementFactory;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;

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

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;

        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

        SampleControl control = new SampleControl();
        mbs.registerMBean(control, new ObjectName(OBJECT_NAME));

        SampleMarkupStandardMBean markup = new SampleMarkupStandardMBean(new SampleMarkup());
        mbs.registerMBean(markup, new ObjectName(MARKUP_OBJECT_NAME));

        JMXServiceURL url = new JMXServiceURL("service:jmx:jmxmp://0.0.0.0:" + port);
        JMXConnectorServer connector = JMXConnectorServerFactory.newJMXConnectorServer(url, null, mbs);
        connector.start();

        System.out.println("Sample target JVM ready.");
        System.out.println("  PID:        " + ProcessHandle.current().pid());
        System.out.println("  JMX URL:    " + url);
        System.out.println("  Connect:    localhost:" + port);
        System.out.println("  MBean:      " + OBJECT_NAME);
        System.out.println("              " + MARKUP_OBJECT_NAME);
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
