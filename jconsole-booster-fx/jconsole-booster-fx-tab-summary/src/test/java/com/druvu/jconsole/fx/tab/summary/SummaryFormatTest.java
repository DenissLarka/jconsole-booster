package com.druvu.jconsole.fx.tab.summary;

import static org.assertj.core.api.Assertions.assertThat;

import com.druvu.jconsole.core.text.Messages;
import java.util.List;
import org.testng.annotations.Test;

/** Covers the texts of the summary: unavailable values, collector lines, paths and arguments one per line. */
public class SummaryFormatTest {

    @Test
    public void aNegativeValueIsUnavailable() {
        assertThat(SummaryFormat.count(-1)).isEqualTo(Messages.UNAVAILABLE);
        assertThat(SummaryFormat.kbytes(-1)).isEqualTo(Messages.UNAVAILABLE);
        assertThat(SummaryFormat.millis(-1)).isEqualTo(Messages.UNAVAILABLE);
        assertThat(SummaryFormat.nanos(-1)).isEqualTo(Messages.UNAVAILABLE);
        assertThat(SummaryFormat.collections(-1, 10)).isEqualTo(Messages.UNAVAILABLE);
    }

    @Test
    public void sizesAreInKilobytes() {
        assertThat(SummaryFormat.kbytes(2048)).isEqualTo("2 kbytes");
    }

    @Test
    public void aCollectorLineSaysHowManyCollectionsAndHowLong() {
        assertThat(SummaryFormat.collections(1, 0)).startsWith("1 collection, ");
        assertThat(SummaryFormat.collections(12, -1)).isEqualTo("12 collections, " + Messages.UNAVAILABLE);
    }

    @Test
    public void aPathIsSplitAtTheTargetsSeparator() {
        assertThat(SummaryFormat.pathEntries("a.jar:b.jar", ":")).isEqualTo("a.jar\nb.jar");
        assertThat(SummaryFormat.pathEntries("C:\\a.jar;C:\\b.jar", ";")).isEqualTo("C:\\a.jar\nC:\\b.jar");
        assertThat(SummaryFormat.pathEntries("", ":")).isEmpty();
        assertThat(SummaryFormat.pathEntries(null, ":")).isEmpty();
    }

    @Test
    public void theSeparatorFollowsTheTargetsOperatingSystem() {
        assertThat(SummaryFormat.pathSeparatorOf("Windows 11")).isEqualTo(";");
        assertThat(SummaryFormat.pathSeparatorOf("Linux")).isEqualTo(":");
        assertThat(SummaryFormat.pathSeparatorOf("Mac OS X")).isEqualTo(":");
    }

    @Test
    public void argumentsAreOnePerLine() {
        assertThat(SummaryFormat.arguments(List.of("-Xmx1g", "-Dx=y"))).isEqualTo("-Xmx1g\n-Dx=y");
        assertThat(SummaryFormat.arguments(List.of())).isEmpty();
    }
}
