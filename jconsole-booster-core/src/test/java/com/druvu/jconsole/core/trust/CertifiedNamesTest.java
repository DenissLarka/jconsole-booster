package com.druvu.jconsole.core.trust;

import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

/** The host-name match of a certificate entry, and the host part of {@code host:port}. */
public class CertifiedNamesTest {

    @Test
    public void anEqualNameMatchesWhateverTheCase() {
        assertThat(CertifiedNames.nameMatches("JMX.Example.com", "jmx.example.COM"))
                .isTrue();
        assertThat(CertifiedNames.nameMatches("jmx.example.com.", "jmx.example.com"))
                .isTrue();
    }

    @Test
    public void anotherNameDoesNotMatch() {
        assertThat(CertifiedNames.nameMatches("other.example.org", "jmx.example.com"))
                .isFalse();
        assertThat(CertifiedNames.nameMatches("example.com", "jmx.example.com")).isFalse();
    }

    @Test
    public void aWildcardStandsForExactlyOneLabelOnTheLeft() {
        assertThat(CertifiedNames.nameMatches("*.example.com", "jmx.example.com"))
                .isTrue();
        assertThat(CertifiedNames.nameMatches("*.example.com", "a.b.example.com"))
                .isFalse();
        assertThat(CertifiedNames.nameMatches("*.example.com", "example.com")).isFalse();
        assertThat(CertifiedNames.nameMatches("*.example.com", ".example.com")).isFalse();
    }

    @Test
    public void aWildcardOverATopLevelDomainMatchesNothing() {
        assertThat(CertifiedNames.nameMatches("*.com", "example.com")).isFalse();
    }

    @Test
    public void aWildcardInsideALabelIsNotAWildcard() {
        assertThat(CertifiedNames.nameMatches("jmx*.example.com", "jmx1.example.com"))
                .isFalse();
        assertThat(CertifiedNames.nameMatches("*jmx.example.com", "ajmx.example.com"))
                .isFalse();
    }

    @Test
    @SuppressWarnings("PMD.AvoidUsingHardCodedIP") // an address literal is what this test reads
    public void theHostOfHostPort() {
        assertThat(CertifiedNames.hostOf("jmx.example.com:7091")).isEqualTo("jmx.example.com");
        assertThat(CertifiedNames.hostOf("[::1]:7091")).isEqualTo("::1");
        assertThat(CertifiedNames.hostOf("localhost")).isEqualTo("localhost");
    }
}
