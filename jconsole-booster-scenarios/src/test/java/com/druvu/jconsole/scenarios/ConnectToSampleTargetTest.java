package com.druvu.jconsole.scenarios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.druvu.jconsole.core.connect.JMXConnectionManager;
import com.druvu.jconsole.core.trust.CertTrustPrompt;
import com.druvu.jconsole.core.vm.ConnectionState;
import com.druvu.jconsole.core.vm.MemoryPoolProxy;
import com.druvu.jconsole.core.vm.ProxyClient;
import com.druvu.jconsole.sampletarget.SampleTargetJvm;
import com.druvu.jconsole.scenarios.fixtures.RunningSampleTarget;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import javax.management.JMException;
import javax.management.MBeanInfo;
import javax.management.MBeanParameterInfo;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * The desktop app's connection to the sample target JVM over JMXMP, end to end: the certificate trusted once, the
 * platform views the tabs read, the markup in the sample MBeans' descriptions, a reconnect, a refused password.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
public class ConnectToSampleTargetTest {

    private RunningSampleTarget target;

    @BeforeClass
    public void startTheTarget() throws JMException, IOException {
        target = RunningSampleTarget.start();
        // Trusted for this session only, as "Trust once" does: no pin is written to the user's files.
        JMXConnectionManager.setCertTrustPrompt((hostPort, cert, fingerprint) -> CertTrustPrompt.Decision.TRUST_ONCE);
    }

    @AfterClass(alwaysRun = true)
    public void stopTheTarget() throws JMException, IOException {
        JMXConnectionManager.setCertTrustPrompt((hostPort, cert, fingerprint) -> CertTrustPrompt.Decision.CANCEL);
        if (target != null) {
            target.close();
        }
    }

    @Test
    public void theTabsReadEveryPlatformViewOfTheTarget() throws IOException {
        ProxyClient client = connect("admin");
        try {
            assertThat(client.getConnectionState()).isEqualTo(ConnectionState.CONNECTED);
            assertThat(client.getRuntimeMXBean().getUptime()).isPositive();
            assertThat(client.getMemoryMXBean().getHeapMemoryUsage().getUsed()).isPositive();
            assertThat(client.getThreadMXBean().getThreadCount()).isPositive();
            assertThat(client.getClassLoadingMXBean().getLoadedClassCount()).isPositive();
            assertThat(client.getOperatingSystemMXBean().getAvailableProcessors())
                    .isPositive();
            assertThat(client.getGarbageCollectorMXBeans()).isNotEmpty();
            assertThat(client.getMemoryPoolProxies()).anyMatch(MemoryPoolProxy::isCollectedMemoryPool);
        } finally {
            client.disconnect();
        }
        assertThat(client.getConnectionState()).isEqualTo(ConnectionState.DISCONNECTED);
    }

    @Test
    public void theListsHandedToTheTabsCannotBeChanged() throws IOException {
        ProxyClient client = connect("admin");
        try {
            assertThatThrownBy(() -> client.getMemoryPoolProxies().clear())
                    .isInstanceOf(UnsupportedOperationException.class);
            assertThatThrownBy(() -> client.getGarbageCollectorMXBeans().clear())
                    .isInstanceOf(UnsupportedOperationException.class);
        } finally {
            client.disconnect();
        }
    }

    @Test
    public void theMarkupInTheSampleDescriptionsReachesTheClient() throws IOException, MalformedObjectNameException {
        ProxyClient client = connect("admin");
        try {
            Map<ObjectName, MBeanInfo> sampleBeans = client.getMBeans("com.druvu.testjvm");
            assertThat(sampleBeans.keySet()).containsExactlyInAnyOrderElementsOf(RunningSampleTarget.sampleMBeans());
            MBeanInfo markup = sampleBeans.get(ObjectName.getInstance(SampleTargetJvm.MARKUP_OBJECT_NAME));
            assertThat(Arrays.stream(markup.getOperations())
                            .flatMap(operation -> Arrays.stream(operation.getSignature()))
                            .map(MBeanParameterInfo::getDescription))
                    .anyMatch(description -> description.contains("{{combo:EURUSD"));
        } finally {
            client.disconnect();
        }
    }

    @Test
    public void aReconnectReadsTheMemoryPoolsOverTheNewConnection() throws IOException {
        ProxyClient client = connect("admin");
        try {
            client.getMemoryPoolProxies().iterator().next().getStat();
        } finally {
            client.disconnect();
        }
        client.connect();
        try {
            for (MemoryPoolProxy pool : client.getMemoryPoolProxies()) {
                assertThat(pool.getStat().getUsage()).isNotNull();
            }
        } finally {
            client.disconnect();
        }
    }

    @Test
    public void aWrongPasswordLeavesTheClientDisconnected() throws IOException {
        ProxyClient client = connect("wrong");
        assertThat(client.getConnectionState()).isEqualTo(ConnectionState.DISCONNECTED);
        assertThat(client.getLastConnectException()).isNotNull();
    }

    private ProxyClient connect(String password) throws IOException {
        ProxyClient client = ProxyClient.getProxyClient(target.url(), "admin", password, false);
        client.connect();
        return client;
    }
}
