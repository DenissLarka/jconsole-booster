package com.druvu.jconsole.core.trust;

import java.net.InetAddress;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * The host names and addresses a certificate is issued for: its subject alternative names, as browsers read them (the
 * subject's common name is not one of them). A trusted authority's signature says who the certificate belongs to; these
 * names say which hosts it may be used for.
 *
 * <p>The match is strict on purpose, because a miss only leads to a question: a name matches when equal (case aside), a
 * wildcard stands for one whole label on the left ({@code *.example.com} covers {@code jmx.example.com}, not
 * {@code example.com} or {@code a.b.example.com}), and an address matches only an address entry.
 *
 * @author Deniss Larka <br>
 *     on 08 Nov 2026
 */
final class CertifiedNames {

    // the entry types of X509Certificate.getSubjectAlternativeNames (RFC 5280 GeneralName)
    private static final int DNS_NAME = 2;
    private static final int IP_ADDRESS = 7;

    private CertifiedNames() {}

    /** The names and addresses the certificate is issued for, in its order; empty when it lists none. */
    static List<String> of(X509Certificate certificate) {
        List<String> names = new ArrayList<>();
        for (List<?> entry : alternativeNames(certificate)) {
            int type = (Integer) entry.get(0);
            if (type == DNS_NAME || type == IP_ADDRESS) {
                names.add((String) entry.get(1));
            }
        }
        return names;
    }

    /** Whether the certificate is issued for {@code host}, a name or an address as the user typed it. */
    static boolean cover(X509Certificate certificate, String host) {
        String wanted = normalized(host);
        InetAddress address = addressOrNull(wanted);
        for (List<?> entry : alternativeNames(certificate)) {
            int type = (Integer) entry.get(0);
            String value = (String) entry.get(1);
            if (address != null && type == IP_ADDRESS && address.equals(addressOrNull(value))) {
                return true;
            }
            if (address == null && type == DNS_NAME && nameMatches(value, wanted)) {
                return true;
            }
        }
        return false;
    }

    /** Whether one DNS entry of a certificate, a name or a left-hand wildcard, matches the host name. */
    static boolean nameMatches(String entry, String host) {
        String pattern = normalized(entry);
        String name = normalized(host);
        if (pattern.equals(name)) {
            return true;
        }
        if (!pattern.startsWith("*.")) {
            return false;
        }
        String parent = pattern.substring(2);
        // a wildcard over a single label ("*.com") would cover a whole top-level domain
        if (!parent.contains(".")) {
            return false;
        }
        int firstDot = name.indexOf('.');
        return firstDot > 0 && name.substring(firstDot + 1).equals(parent);
    }

    /** The host part of {@code host:port}, without the brackets of an IPv6 address. */
    static String hostOf(String hostPort) {
        int colon = hostPort.lastIndexOf(':');
        String host = colon < 0 ? hostPort : hostPort.substring(0, colon);
        if (host.startsWith("[") && host.endsWith("]")) {
            return host.substring(1, host.length() - 1);
        }
        return host;
    }

    private static Collection<List<?>> alternativeNames(X509Certificate certificate) {
        try {
            Collection<List<?>> names = certificate.getSubjectAlternativeNames();
            return names == null ? List.of() : names;
        } catch (CertificateParsingException e) {
            // an unreadable extension names no host: the certificate goes to the question
            return List.of();
        }
    }

    private static String normalized(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        // "host." and "host" are the same name in DNS
        return lower.endsWith(".") ? lower.substring(0, lower.length() - 1) : lower;
    }

    /** The address a literal stands for, or {@code null} for a host name (no lookup is made). */
    private static InetAddress addressOrNull(String literal) {
        try {
            return InetAddress.ofLiteral(literal);
        } catch (IllegalArgumentException notAnAddress) {
            return null;
        }
    }
}
