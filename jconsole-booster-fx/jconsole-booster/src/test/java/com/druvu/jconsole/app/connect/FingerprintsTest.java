package com.druvu.jconsole.app.connect;

import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

/** Covers how a stored fingerprint is shown: hex pairs with colons, an odd tail and no fingerprint included. */
public class FingerprintsTest {

    @Test
    public void pairsAreSeparatedByColons() {
        assertThat(Fingerprints.withColons("AB12CD")).isEqualTo("AB:12:CD");
        assertThat(Fingerprints.withColons("ABC")).isEqualTo("AB:C");
        assertThat(Fingerprints.withColons("")).isEmpty();
        assertThat(Fingerprints.withColons(null)).isEmpty();
    }
}
