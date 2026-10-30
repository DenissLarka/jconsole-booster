package com.druvu.jconsole.app.connect;

import com.druvu.jconsole.core.launch.ArgumentParser;
import java.util.Objects;

/**
 * What the user asked to connect to.
 *
 * @param url the full service URL ({@code service:jmx:...})
 * @param userName the user name, or {@code null} for none
 * @param password the password, or {@code null} for none
 * @param requireValidChain when {@code true}, a certificate that does not chain to a trusted CA is refused instead of
 *     offered for trust on first use
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
public record ConnectionRequest(String url, String userName, String password, boolean requireValidChain) {

    public ConnectionRequest {
        Objects.requireNonNull(url, "url");
    }

    /**
     * A request without credentials for what the user typed or a bookmark holds: {@code host:port} or a service URL.
     */
    public static ConnectionRequest to(String target) {
        return new ConnectionRequest(ArgumentParser.adaptUrl(target.strip()), null, null, false);
    }

    /** The URL as the user reads it: {@code host:port} for the default JMXMP, the service URL otherwise. */
    public String shortUrl() {
        return ArgumentParser.shortenUrl(url);
    }

    /**
     * The form in which the recent connections and the bookmarks file both store a target, so the two compare as plain
     * strings: {@code host:port} for the default JMXMP, the service URL otherwise.
     */
    public static String canonical(String target) {
        return target == null || target.isBlank() ? "" : to(target).shortUrl();
    }

    /** Never the password: a request may end up in a log. */
    @Override
    public String toString() {
        return "ConnectionRequest[" + (userName == null ? "" : userName + "@") + url + "]";
    }
}
