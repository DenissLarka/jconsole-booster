package com.druvu.jconsole.app.connect;

/**
 * Shows a certificate fingerprint the way people compare them: hex pairs separated by colons.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
final class Fingerprints {

    private Fingerprints() {}

    /** {@code AB12CD} becomes {@code AB:12:CD}; the pinned fingerprints are stored without colons. */
    static String withColons(String hex) {
        if (hex == null) {
            return "";
        }
        StringBuilder pairs = new StringBuilder(hex.length() * 3 / 2);
        for (int i = 0; i < hex.length(); i += 2) {
            if (i > 0) {
                pairs.append(':');
            }
            pairs.append(hex, i, Math.min(i + 2, hex.length()));
        }
        return pairs.toString();
    }
}
