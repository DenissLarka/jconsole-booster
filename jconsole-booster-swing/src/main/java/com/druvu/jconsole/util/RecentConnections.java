package com.druvu.jconsole.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * Most-recently-used list of successful connection URLs, persisted in {@link BoosterHome#recentConnectionsFile()} (one
 * URL per line, most-recent-first, de-duplicated, capped at {@link #MAX}).
 *
 * <p>Only the URL is stored — never credentials. All I/O is best-effort: a read/write failure is logged at WARN and
 * never propagated, so a broken or unwritable config home can never block a connection.
 */
@Slf4j
public final class RecentConnections {

    public static final int MAX = 10;

    private RecentConnections() {}

    /** Returns the stored URLs, most-recent-first (empty list if none / unreadable). */
    public static synchronized List<String> load() {
        Path file = BoosterHome.recentConnectionsFile();
        if (!Files.exists(file)) {
            return new ArrayList<>();
        }
        List<String> out = new ArrayList<>();
        try {
            for (String raw : Files.readAllLines(file)) {
                String s = raw.strip();
                if (!s.isEmpty() && !out.contains(s)) {
                    out.add(s);
                    if (out.size() >= MAX) {
                        break;
                    }
                }
            }
        } catch (IOException e) {
            log.warn("Could not read {}: {}", file, e.getMessage());
        }
        return out;
    }

    /** Records a successful connection URL: moves it to the front, de-dupes, trims to {@link #MAX}, persists. */
    public static synchronized void record(String url) {
        if (url == null || url.isBlank()) {
            return;
        }
        String u = url.strip();
        List<String> list = load();
        list.remove(u);
        list.add(0, u);
        if (list.size() > MAX) {
            list = new ArrayList<>(list.subList(0, MAX));
        }
        Path file = BoosterHome.recentConnectionsFile();
        try {
            Files.write(file, list);
        } catch (IOException e) {
            log.warn("Could not write {}: {}", file, e.getMessage());
        }
    }
}
