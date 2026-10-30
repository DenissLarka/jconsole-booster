package com.druvu.jconsole.jmx;

import com.druvu.jconsole.util.BoosterHome;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
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

    /**
     * @return every fingerprint currently pinned for {@code hostPort} (persistent ∪ session), canonical hex. Empty if
     *     the host is unknown. A non-empty result whose members all differ from a freshly presented fingerprint is the
     *     "certificate changed" signal.
     */
    public Set<String> pinnedFingerprints(String hostPort) {
        ensureLoaded();
        Set<String> all = new HashSet<>();
        Set<String> p = persisted.get(hostPort);
        if (p != null) {
            all.addAll(p);
        }
        Set<String> s = session.get(hostPort);
        if (s != null) {
            all.addAll(s);
        }
        return all;
    }

    /**
     * Replace ALL pins for {@code hostPort} with a single new {@code fingerprint}: the persistent pin becomes the new
     * one (old lines for this host removed from {@code trusted-certs.txt}, comments and other hosts preserved
     * verbatim), and any session pins for the host are cleared. Used on the "certificate changed" path so a rotated
     * cert does not leave the superseded fingerprint trusted, and the file does not accumulate one stale pin per server
     * restart.
     */
    public void replacePersistent(String hostPort, String fingerprint) {
        ensureLoaded();
        Set<String> set = ConcurrentHashMap.newKeySet();
        set.add(canon(fingerprint));
        persisted.put(hostPort, set);
        session.remove(hostPort);
        rewriteReplacing(hostPort, hostPort + "\t" + fingerprint + "\t# updated " + LocalDate.now());
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

    /**
     * Rewrites {@code trusted-certs.txt}, dropping every pin line whose first token equals {@code hostPort} and
     * appending {@code newLine}. Comments ({@code #…}), blank lines, and other hosts' lines are kept verbatim.
     */
    private synchronized void rewriteReplacing(String hostPort, String newLine) {
        try {
            List<String> out = new ArrayList<>();
            if (Files.exists(file)) {
                for (String raw : Files.readAllLines(file)) {
                    String line = raw.trim();
                    if (line.isEmpty() || line.startsWith("#")) {
                        out.add(raw);
                        continue;
                    }
                    String[] parts = line.split("\\s+");
                    if (parts.length >= 1 && parts[0].equals(hostPort)) {
                        continue; // superseded pin for this host — drop it
                    }
                    out.add(raw);
                }
            }
            out.add(newLine);
            Path parent = file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            // Write to a sibling temp file then move into place, so a crash mid-write can never truncate the trust
            // file (which would silently revert every host to the quiet first-use prompt).
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.write(tmp, out);
            try {
                Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException notAtomic) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not rewrite trusted certificates file: " + file, e);
        }
    }

    // synchronized on the same monitor as rewriteReplacing so an append can never interleave with a full-file
    // rewrite (which reads-all-then-truncates) and be silently lost.
    private synchronized void append(String hostPort, String fingerprint) {
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
