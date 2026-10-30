package com.druvu.jconsole.core.trust;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.testng.Assert;
import org.testng.annotations.Test;

/** {@link TrustedCertStore}: persistent pins against session pins, the file's other lines kept on a replace. */
public class TrustedCertStoreTest {

    @Test
    public void persistentPinSurvivesReloadAndIsColonInsensitive() throws IOException {
        Path tmp = Files.createTempFile("trusted-certs", ".txt");
        Files.deleteIfExists(tmp);
        try {
            TrustedCertStore store = new TrustedCertStore(tmp);
            Assert.assertFalse(store.isTrusted("host:7091", "AA:BB:CC"));
            store.rememberPersistent("host:7091", "AA:BB:CC");
            Assert.assertTrue(new TrustedCertStore(tmp).isTrusted("host:7091", "aabbcc"));
            Assert.assertFalse(new TrustedCertStore(tmp).isTrusted("host:7091", "DD:EE:FF"));
            Assert.assertFalse(new TrustedCertStore(tmp).isTrusted("other:7091", "AA:BB:CC"));
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    @Test
    public void sessionPinIsNotPersisted() throws IOException {
        Path tmp = Files.createTempFile("trusted-certs", ".txt");
        Files.deleteIfExists(tmp);
        try {
            TrustedCertStore store = new TrustedCertStore(tmp);
            store.rememberForSession("host:7091", "AA:BB:CC");
            Assert.assertTrue(store.isTrusted("host:7091", "AA:BB:CC"));
            Assert.assertFalse(new TrustedCertStore(tmp).isTrusted("host:7091", "AA:BB:CC"));
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    @Test
    public void pinnedFingerprintsUnionsPersistentAndSession() throws IOException {
        Path tmp = Files.createTempFile("trusted-certs", ".txt");
        Files.deleteIfExists(tmp);
        try {
            TrustedCertStore store = new TrustedCertStore(tmp);
            store.rememberPersistent("host:7091", "AA:BB");
            store.rememberForSession("host:7091", "CC:DD");
            Set<String> pins = store.pinnedFingerprints("host:7091");
            Assert.assertEquals(pins.size(), 2);
            Assert.assertTrue(pins.contains("AABB"), "persistent pin present (canonical form)");
            Assert.assertTrue(pins.contains("CCDD"), "session pin present (canonical form)");
            Assert.assertTrue(store.pinnedFingerprints("unknown:1").isEmpty(), "unknown host → empty");
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    @Test
    public void replacePersistentSwapsPinAndPreservesOtherLines() throws IOException {
        Path tmp = Files.createTempFile("trusted-certs", ".txt");
        try {
            Files.write(tmp, List.of("# my trusted servers", "host:7091\tAA:AA:AA", "other:9000\tBB:BB:BB"));
            new TrustedCertStore(tmp).replacePersistent("host:7091", "CC:CC:CC");

            TrustedCertStore reloaded = new TrustedCertStore(tmp);
            Assert.assertTrue(reloaded.isTrusted("host:7091", "CC:CC:CC"), "new pin trusted after reload");
            Assert.assertFalse(reloaded.isTrusted("host:7091", "AA:AA:AA"), "superseded pin removed");
            Assert.assertTrue(reloaded.isTrusted("other:9000", "BB:BB:BB"), "unrelated host preserved");

            List<String> lines = Files.readAllLines(tmp);
            Assert.assertTrue(
                    lines.stream().anyMatch(l -> l.startsWith("# my trusted servers")), "comment line preserved");
            Assert.assertEquals(
                    lines.stream().filter(l -> l.trim().startsWith("host:7091")).count(),
                    1,
                    "exactly one pin line for the changed host (no accumulation)");
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    @Test
    public void replacePersistentClearsStaleSessionPin() throws IOException {
        Path tmp = Files.createTempFile("trusted-certs", ".txt");
        Files.deleteIfExists(tmp);
        try {
            TrustedCertStore store = new TrustedCertStore(tmp);
            store.rememberForSession("host:7091", "AA:AA");
            Assert.assertTrue(store.isTrusted("host:7091", "AA:AA"));
            store.replacePersistent("host:7091", "BB:BB");
            Assert.assertFalse(store.isTrusted("host:7091", "AA:AA"), "stale session pin cleared on replace");
            Assert.assertTrue(store.isTrusted("host:7091", "BB:BB"));
        } finally {
            Files.deleteIfExists(tmp);
        }
    }
}
