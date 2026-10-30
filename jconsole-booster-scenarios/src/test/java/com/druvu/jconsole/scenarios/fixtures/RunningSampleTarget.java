package com.druvu.jconsole.scenarios.fixtures;

import com.druvu.jconsole.sampletarget.SampleControl;
import com.druvu.jconsole.sampletarget.SampleTargetJvm;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.ServerSocket;
import java.util.List;
import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnectorServer;

/** The sample target inside the test JVM: its MBeans on the platform server, its secured endpoint on a free port. */
public final class RunningSampleTarget implements AutoCloseable {

    private final MBeanServer mbs;
    private final JMXConnectorServer endpoint;
    private final int port;

    private RunningSampleTarget(MBeanServer mbs, JMXConnectorServer endpoint, int port) {
        this.mbs = mbs;
        this.endpoint = endpoint;
        this.port = port;
    }

    /** Registers the sample MBeans on this JVM's platform server and starts the endpoint on localhost. */
    public static RunningSampleTarget start() throws JMException, IOException {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        SampleTargetJvm.registerMBeans(mbs, new SampleControl());
        int port = freePort();
        return new RunningSampleTarget(mbs, SampleTargetJvm.startSecuredEndpoint(mbs, "localhost", port), port);
    }

    /** The names of the three sample MBeans. */
    public static List<ObjectName> sampleMBeans() throws MalformedObjectNameException {
        return List.of(
                ObjectName.getInstance(SampleTargetJvm.OBJECT_NAME),
                ObjectName.getInstance(SampleTargetJvm.MARKUP_OBJECT_NAME),
                ObjectName.getInstance(SampleTargetJvm.DOWNLOADS_OBJECT_NAME));
    }

    /** The JMXMP service URL a client connects to. */
    public String url() {
        return "service:jmx:jmxmp://localhost:" + port;
    }

    @Override
    public void close() throws JMException, IOException {
        endpoint.stop();
        for (ObjectName name : sampleMBeans()) {
            mbs.unregisterMBean(name);
        }
    }

    private static int freePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
