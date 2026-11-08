package com.druvu.jconsole.core.connect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.ServerSocket;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import javax.management.MBeanServerFactory;
import javax.management.remote.JMXAuthenticator;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXPrincipal;
import javax.management.remote.JMXServiceURL;
import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;
import javax.security.auth.Subject;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** RMI, the JDK's own connector: a password goes only to a server whose connector encrypts its calls. */
public class RmiConnectTest {

    private final AtomicInteger logins = new AtomicInteger();
    private Registry registry;
    private JMXConnectorServer server;
    private JMXServiceURL url;

    @BeforeMethod
    public void startRegistry() throws IOException {
        int port;
        try (ServerSocket probe = new ServerSocket(0)) {
            port = probe.getLocalPort();
        }
        registry = LocateRegistry.createRegistry(port);
        url = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://localhost:" + port + "/jmxrmi");
    }

    @AfterMethod(alwaysRun = true)
    public void stop() throws IOException {
        if (server != null) {
            server.stop();
        }
        UnicastRemoteObject.unexportObject(registry, true);
    }

    @Test
    public void aPasswordIsNotSentToAnUnencryptedRmiServer() throws IOException {
        startServer(true);

        assertThatThrownBy(() -> JMXConnectionManager.connect(url, "admin", "secret", null, false))
                .isInstanceOf(PlaintextCredentialsRefusedException.class);
        assertThat(logins.get()).as("the server saw the password").isZero();
    }

    @Test
    public void anAnonymousRmiServerStillConnects() throws IOException {
        startServer(false);

        JMXConnectionManager.ConnectionResult result = JMXConnectionManager.connect(url, null, null, null, false);

        assertThat(result.connection().getMBeanCount()).isPositive();
        result.connector().close();
    }

    @Test
    public void aStubExportedWithSslIsEncrypted() throws IOException {
        Ping ping = new PingImpl();
        Remote stub = UnicastRemoteObject.exportObject(
                ping, 0, new SslRMIClientSocketFactory(), new SslRMIServerSocketFactory());
        try {
            assertThat(RmiConnect.encrypts(stub)).isTrue();
        } finally {
            UnicastRemoteObject.unexportObject(ping, true);
        }
    }

    @Test
    public void aStubExportedPlainIsNotEncrypted() throws IOException {
        Ping ping = new PingImpl();
        Remote stub = UnicastRemoteObject.exportObject(ping, 0);
        try {
            assertThat(RmiConnect.encrypts(stub)).isFalse();
        } finally {
            UnicastRemoteObject.unexportObject(ping, true);
        }
    }

    @Test
    public void theEndpointOfARegistryAddress() throws IOException {
        assertThat(RmiConnect.endpoint(new JMXServiceURL("service:jmx:rmi:///jndi/rmi://prod-host:9010/jmxrmi")))
                .isEqualTo("prod-host:9010");
        assertThat(RmiConnect.endpoint(new JMXServiceURL("service:jmx:rmi:///jndi/rmi://prod-host/jmxrmi")))
                .isEqualTo("prod-host:1099");
    }

    /** A remote object to export: its stub carries the socket factory it was exported with. */
    public interface Ping extends Remote {
        void ping() throws RemoteException;
    }

    private static final class PingImpl implements Ping {
        @Override
        public void ping() {
            // nothing: only the stub is looked at
        }
    }

    private void startServer(boolean withLogin) throws IOException {
        Map<String, Object> env = new HashMap<>();
        if (withLogin) {
            JMXAuthenticator authenticator = credentials -> {
                logins.incrementAndGet();
                return new Subject(true, Set.of(new JMXPrincipal("admin")), Set.of(), Set.of());
            };
            env.put(JMXConnectorServer.AUTHENTICATOR, authenticator);
        }
        server = JMXConnectorServerFactory.newJMXConnectorServer(url, env, MBeanServerFactory.newMBeanServer());
        server.start();
    }
}
