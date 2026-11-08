package com.druvu.jconsole.core.connect;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RemoteObject;
import java.rmi.server.RemoteRef;
import java.util.Map;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.rmi.RMIConnector;
import javax.management.remote.rmi.RMIServer;
import javax.rmi.ssl.SslRMIClientSocketFactory;

/**
 * Connects over RMI with a user name and password only when the server's connector encrypts its calls.
 *
 * <p>The JDK's RMI connector sends the password in its first call to the server, over whatever socket the server's stub
 * names: a plain one unless the server runs with SSL. So the stub is looked up first, without credentials, and the
 * socket factory it carries is read: {@link SslRMIClientSocketFactory} means encrypted, anything else means the
 * password would cross the network in clear text, and the connection is refused before it leaves.
 *
 * <p>Only the usual address form, {@code service:jmx:rmi:///jndi/rmi://host:port/name}, can be checked this way; any
 * other RMI address is refused when credentials are given.
 *
 * @author Deniss Larka <br>
 *     on 08 Nov 2026
 */
final class RmiConnect {

    private static final String REGISTRY_PREFIX = "/jndi/rmi://";

    private RmiConnect() {}

    /** Whether {@code url} is an RMI address (the JDK's connector). */
    static boolean isRmi(JMXServiceURL url) {
        return "rmi".equals(url.getProtocol());
    }

    /**
     * Connects to an RMI server with the credentials in {@code env}, once its stub shows an encrypted connector.
     *
     * @throws PlaintextCredentialsRefusedException when the server's connector does not encrypt, or cannot be checked
     */
    static JMXConnector connectWithCredentials(JMXServiceURL url, Map<String, ?> env) throws IOException {
        URI registryAddress = registryAddress(url);
        if (registryAddress == null) {
            throw new PlaintextCredentialsRefusedException("Refusing to send credentials to " + url + ": only a "
                    + REGISTRY_PREFIX + " address can be checked for encryption first");
        }
        Remote stub = lookUp(registryAddress);
        if (!(stub instanceof RMIServer server)) {
            throw new IOException(registryAddress + " is not a JMX connector");
        }
        if (!encrypts(stub)) {
            throw new PlaintextCredentialsRefusedException(
                    "Refusing to send credentials to a server whose RMI connector is not encrypted (no SSL)");
        }
        RMIConnector connector = new RMIConnector(server, env);
        connector.connect(env);
        return connector;
    }

    /** {@code host:port} of the RMI registry an address names, for messages; the address itself when it names none. */
    static String endpoint(JMXServiceURL url) {
        URI registryAddress = registryAddress(url);
        return registryAddress == null ? url.toString() : registryAddress.getHost() + ":" + port(registryAddress);
    }

    /**
     * Whether a stub's calls go through SSL: the client socket factory it carries is {@link SslRMIClientSocketFactory}.
     */
    static boolean encrypts(Remote stub) {
        RemoteRef reference = referenceOf(stub);
        if (reference == null) {
            return false;
        }
        // The stub's reference writes its endpoint, the socket factory included, when it is serialized: catching the
        // objects it writes reads the factory through public API only.
        try (FactoryCatcher catcher = new FactoryCatcher()) {
            reference.writeExternal(catcher);
            return catcher.sawSslFactory;
        } catch (IOException e) {
            return false;
        }
    }

    private static RemoteRef referenceOf(Remote stub) {
        if (stub instanceof RemoteObject remote) {
            return remote.getRef();
        }
        if (Proxy.isProxyClass(stub.getClass())) {
            InvocationHandler handler = Proxy.getInvocationHandler(stub);
            if (handler instanceof RemoteObject remote) {
                return remote.getRef();
            }
        }
        return null;
    }

    /** {@code rmi://host:port/name} from {@code service:jmx:rmi:///jndi/rmi://host:port/name}, or {@code null}. */
    private static URI registryAddress(JMXServiceURL url) {
        String path = url.getURLPath();
        if (!path.startsWith(REGISTRY_PREFIX)) {
            return null;
        }
        try {
            URI address = URI.create(path.substring("/jndi/".length()));
            return address.getHost() == null ? null : address;
        } catch (IllegalArgumentException malformed) {
            return null;
        }
    }

    private static Remote lookUp(URI registryAddress) throws IOException {
        String name = registryAddress.getPath() == null
                ? ""
                : registryAddress.getPath().replaceFirst("^/", "");
        try {
            Registry registry = LocateRegistry.getRegistry(registryAddress.getHost(), port(registryAddress));
            return registry.lookup(name);
        } catch (NotBoundException e) {
            throw new IOException("No JMX connector named '" + name + "' at " + registryAddress, e);
        }
    }

    private static int port(URI registryAddress) {
        return registryAddress.getPort() < 0 ? Registry.REGISTRY_PORT : registryAddress.getPort();
    }

    /** An object stream that writes nothing and notes whether an SSL socket factory passed through it. */
    private static final class FactoryCatcher extends ObjectOutputStream {

        private boolean sawSslFactory;

        FactoryCatcher() throws IOException {
            super(OutputStream.nullOutputStream());
            enableReplaceObject(true);
        }

        @Override
        protected Object replaceObject(Object written) {
            if (written instanceof SslRMIClientSocketFactory) {
                sawSslFactory = true;
            }
            return written;
        }
    }
}
