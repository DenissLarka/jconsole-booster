package com.druvu.jconsole.jmx;

import com.druvu.jconsole.util.BoosterHome;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-certificate fingerprints the operator has chosen to trust, keyed by {@code host:port}.
 *
 * <p>Two tiers: <b>persistent</b> pins live in a hand-editable text file (an SSH {@code known_hosts} analog —
 * {@code trusted-certs.txt} under {@link BoosterHome}), one {@code host:port <sha-256>} per line; <b>session</b> pins
 * are held in memory only and forgotten when JCB exits. Fingerprints are compared canonically (hex only, case
 * insensitive), so hand-edited entries with or without colons both match.
 *
 * <p>Thread-safe: connections (hence trust checks) run on worker threads.
 */
public final class TrustedCertStore {

    private static final TrustedCertStore DEFAULT = new TrustedCertStore(BoosterHome.trustedCertsFile());

    /** The store backed by {@code trusted-certs.txt} under {@link BoosterHome} — used by the running app. */
    public static TrustedCertStore getDefault() {
        return DEFAULT;
    }

    private final Path file;
    private final Map<String, Set<String>> persisted = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> session = new ConcurrentHashMap<>();
    private volatile boolean loaded;

    /**
     * Creates a store backed by {@code file}. Public for testability with a temp file; the app uses
     * {@link #getDefault()}.
     */
    public TrustedCertStore(Path file) {
        this.file = file;
    }

    /** @return {@code true} if {@code fingerprint} is pinned for {@code hostPort} (persistent or session). */
    public boolean isTrusted(String hostPort, String fingerprint) {
        ensureLoaded();
        String fp = canon(fingerprint);
        return has(persisted, hostPort, fp) || has(session, hostPort, fp);
    }

    /** Remember for this JVM session only (not written to disk). */
    public void rememberForSession(String hostPort, String fingerprint) {
        session.computeIfAbsent(hostPort, k -> ConcurrentHashMap.newKeySet()).add(canon(fingerprint));
    }

    /** Pin persistently: add to memory and append a line to {@code trusted-certs.txt}. */
    public void rememberPersistent(String hostPort, String fingerprint) {
        ensureLoaded();
        boolean added = persisted
                .computeIfAbsent(hostPort, k -> ConcurrentHashMap.newKeySet())
                .add(canon(fingerprint));
        if (added) {
            append(hostPort, fingerprint);
        }
    }

    private static boolean has(Map<String, Set<String>> map, String hostPort, String canonFingerprint) {
        Set<String> set = map.get(hostPort);
        return set != null && set.contains(canonFingerprint);
    }

    private synchronized void ensureLoaded() {
        if (loaded) {
            return;
        }
        try {
            if (Files.exists(file)) {
                List<String> lines = Files.readAllLines(file);
                for (String raw : lines) {
                    String line = raw.trim();
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }
                    String[] parts = line.split("\\s+");
                    if (parts.length >= 2) {
                        persisted
                                .computeIfAbsent(parts[0], k -> ConcurrentHashMap.newKeySet())
                                .add(canon(parts[1]));
                    }
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read trusted certificates file: " + file, e);
        }
        loaded = true;
    }

    private void append(String hostPort, String fingerprint) {
        String line = hostPort + "\t" + fingerprint + "\t# trusted " + LocalDate.now() + System.lineSeparator();
        try {
            Path parent = file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(file, line, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not append to trusted certificates file: " + file, e);
        }
    }

    private static String canon(String fingerprint) {
        return fingerprint == null ? "" : fingerprint.toUpperCase(Locale.ROOT).replaceAll("[^0-9A-F]", "");
    }
}
