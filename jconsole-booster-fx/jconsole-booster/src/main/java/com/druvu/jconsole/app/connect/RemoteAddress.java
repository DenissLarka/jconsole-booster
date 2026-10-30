package com.druvu.jconsole.app.connect;

/**
 * Decides whether what the user typed in the Connect dialog can be connected to: a service URL
 * ({@code service:jmx:...}), {@code host:port}, or {@code [ipv6-address]:port}.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
public final class RemoteAddress {

    private static final String SERVICE_URL_PREFIX = "service:jmx:";
    private static final int MAX_PORT = 0xFFFF;

    private RemoteAddress() {}

    /** Whether {@code text} names a remote JVM the app can try to connect to. */
    public static boolean isValid(String text) {
        if (text == null) {
            return false;
        }
        String target = text.strip();
        if (target.startsWith(SERVICE_URL_PREFIX)) {
            return target.length() > SERVICE_URL_PREFIX.length();
        }
        if (target.startsWith("[")) {
            // a literal IPv6 address: [address]:port
            int end = target.indexOf("]:");
            return end > 1 && isPort(target.substring(end + 2));
        }
        String[] hostAndPort = target.split(":");
        return hostAndPort.length == 2 && !hostAndPort[0].isBlank() && isPort(hostAndPort[1]);
    }

    private static boolean isPort(String text) {
        try {
            int port = Integer.parseInt(text);
            return port >= 0 && port <= MAX_PORT;
        } catch (NumberFormatException notANumber) {
            return false;
        }
    }
}
