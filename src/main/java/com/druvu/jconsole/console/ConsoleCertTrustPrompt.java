package com.druvu.jconsole.console;

import com.druvu.jconsole.jmx.CertTrustPrompt;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.List;

/**
 * Console (text) implementation of {@link CertTrustPrompt}: prints the endpoint and certificate details and asks the
 * operator whether to trust it (trust-on-first-use). {@link #confirmChanged} is overridden with a loud warning —
 * without the override the default delegates to {@link #confirm}, silently degrading a certificate-rotation event to a
 * quiet first-use prompt. Anything but an explicit {@code o}/{@code r} defaults to cancel (fail-closed).
 */
final class ConsoleCertTrustPrompt implements CertTrustPrompt {

    private final ConsoleIO io;

    ConsoleCertTrustPrompt(ConsoleIO io) {
        this.io = io;
    }

    @Override
    public Decision confirm(String hostPort, X509Certificate cert, String sha256Fingerprint) {
        io.println();
        io.println("Untrusted server certificate presented by " + hostPort);
        printCert(cert, sha256Fingerprint);
        return ask("Trust? [o]nce / [r]emember / [c]ancel: ");
    }

    @Override
    public Decision confirmChanged(
            String hostPort, X509Certificate cert, String sha256Fingerprint, List<String> previousFingerprints) {
        io.println();
        io.println("!!! CERTIFICATE CHANGED !!!");
        io.println("The certificate for " + hostPort + " differs from the one previously pinned.");
        io.println("This is routine if the server rotated its cert (druvu-lib-jmxmp regenerates an ephemeral");
        io.println("self-signed cert on every restart) — but it can also be an active man-in-the-middle.");
        io.println("Previously pinned:");
        for (String fp : previousFingerprints) {
            io.println("  " + fp);
        }
        io.println("Now presented:");
        printCert(cert, sha256Fingerprint);
        return ask("Replace the pin? [o]nce / [r]emember / [c]ancel: ");
    }

    private void printCert(X509Certificate cert, String sha256Fingerprint) {
        io.println("  subject : " + cert.getSubjectX500Principal().getName());
        io.println("  issuer  : " + cert.getIssuerX500Principal().getName());
        io.println("  valid   : " + cert.getNotBefore() + " .. " + cert.getNotAfter());
        io.println("  SHA-256 : " + sha256Fingerprint);
    }

    private Decision ask(String prompt) {
        io.print(prompt);
        io.out().flush();
        String line;
        try {
            line = io.readLine();
        } catch (IOException e) {
            return Decision.CANCEL;
        }
        if (line == null) {
            return Decision.CANCEL;
        }
        return switch (line.strip().toLowerCase()) {
            case "o", "once" -> Decision.TRUST_ONCE;
            case "r", "remember" -> Decision.TRUST_REMEMBER;
            default -> Decision.CANCEL;
        };
    }
}
