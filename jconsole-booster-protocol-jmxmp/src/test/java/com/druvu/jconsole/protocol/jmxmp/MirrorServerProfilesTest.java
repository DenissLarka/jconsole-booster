package com.druvu.jconsole.protocol.jmxmp;

import com.druvu.jconsole.core.connect.PlaintextCredentialsRefusedException;
import java.util.HashMap;
import java.util.Map;
import org.testng.Assert;
import org.testng.annotations.Test;

/** {@link MirrorServerProfiles}: the server's profiles mirrored, and never credentials over plaintext. */
public class MirrorServerProfilesTest {

    // --- transport auto-negotiation (no credentials → plaintext allowed) ---

    @Test
    public void selectorRequestsTlsSaslWhenServerOffersIt() throws Exception {
        Map<String, Object> env = new HashMap<>();
        new MirrorServerProfiles(false).selectProfiles(env, "TLS SASL/PLAIN");
        Assert.assertEquals(env.get("jmx.remote.profiles"), "TLS SASL/PLAIN");
    }

    @Test
    public void selectorRequestsNoProfilesForPlaintextServer() throws Exception {
        Map<String, Object> env = new HashMap<>();
        env.put("jmx.remote.profiles", "stale");
        new MirrorServerProfiles(false).selectProfiles(env, "");
        Assert.assertFalse(env.containsKey("jmx.remote.profiles"), "plaintext server → no client profiles");
    }

    @Test
    public void selectorIgnoresProfilesThisBuildDoesNotSpeak() throws Exception {
        Map<String, Object> env = new HashMap<>();
        new MirrorServerProfiles(false).selectProfiles(env, "SASL/CRAM-MD5 TLS SASL/PLAIN");
        Assert.assertEquals(env.get("jmx.remote.profiles"), "TLS SASL/PLAIN");
    }

    // --- never send credentials over plaintext (hard block) ---

    @Test(expectedExceptions = PlaintextCredentialsRefusedException.class)
    public void credentialedPlaintextIsRefusedBeforeSasl() throws Exception {
        // Credentials present + server offers no encryption → the selector throws (before any SASL/PLAIN send).
        new MirrorServerProfiles(true).selectProfiles(new HashMap<>(), "");
    }

    @Test
    public void credentialedTlsIsAllowed() throws Exception {
        Map<String, Object> env = new HashMap<>();
        new MirrorServerProfiles(true).selectProfiles(env, "TLS SASL/PLAIN");
        Assert.assertEquals(env.get("jmx.remote.profiles"), "TLS SASL/PLAIN", "encrypted → credentials may flow");
    }

    @Test
    public void anonymousPlaintextIsAllowed() throws Exception {
        // No credentials → a legacy plaintext server is fine (only session-hijack risk, no secret to leak).
        Map<String, Object> env = new HashMap<>();
        new MirrorServerProfiles(false).selectProfiles(env, "");
        Assert.assertFalse(env.containsKey("jmx.remote.profiles"), "anonymous plaintext connects in the clear");
    }
}
