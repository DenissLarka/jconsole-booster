package com.druvu.jconsole;

import com.druvu.jconsole.launcher.ArgumentParser;
import com.druvu.jconsole.launcher.JConsoleOptions;
import java.util.Optional;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Tests for {@link ArgumentParser} URL shorthand expansion and option parsing. */
public class TestArgumentParser {

    @Test
    public void bareHostColonPortDefaultsToJmxmp() {
        String result = ArgumentParser.adaptUrl("localhost:7091");
        Assert.assertEquals(result, ArgumentParser.JMXMP_PREFIX + "localhost:7091");
    }

    @Test
    public void fullJmxmpUrlPassesThrough() {
        String url = "service:jmx:jmxmp://remotehost:5000";
        Assert.assertEquals(ArgumentParser.adaptUrl(url), url);
    }

    // ----- parse() smoke / option-handling tests -----

    @Test
    public void emptyArgsYieldDefaultOptions() {
        Optional<JConsoleOptions> opts = ArgumentParser.parse(new String[0]);
        Assert.assertTrue(opts.isPresent(), "empty args should produce default options");
        JConsoleOptions o = opts.get();
        Assert.assertFalse(o.debug());
        Assert.assertFalse(o.noTile());
        Assert.assertEquals(o.updateInterval(), 4000);
        Assert.assertNull(o.color());
        Assert.assertTrue(o.urls().isEmpty());
    }

    @Test
    public void versionFlagShortCircuits() {
        // -version prints the version banner and signals the launcher to exit
        Assert.assertTrue(ArgumentParser.parse(new String[] {"-version"}).isEmpty());
        Assert.assertTrue(ArgumentParser.parse(new String[] {"-fullversion"}).isEmpty());
    }

    @Test
    public void helpFlagShortCircuits() {
        Assert.assertTrue(ArgumentParser.parse(new String[] {"-h"}).isEmpty());
        Assert.assertTrue(ArgumentParser.parse(new String[] {"-help"}).isEmpty());
        Assert.assertTrue(ArgumentParser.parse(new String[] {"-?"}).isEmpty());
    }

    @Test
    public void debugFlagIsRecorded() {
        JConsoleOptions o = ArgumentParser.parse(new String[] {"-debug"}).orElseThrow();
        Assert.assertTrue(o.debug());
    }

    @Test
    public void notileFlagIsRecorded() {
        JConsoleOptions o = ArgumentParser.parse(new String[] {"-notile"}).orElseThrow();
        Assert.assertTrue(o.noTile());
    }

    @Test
    public void intervalFlagIsConvertedToMillis() {
        JConsoleOptions o = ArgumentParser.parse(new String[] {"-interval=10"}).orElseThrow();
        Assert.assertEquals(o.updateInterval(), 10_000);
    }

    @Test
    public void zeroOrNegativeIntervalIsRejected() {
        Assert.assertTrue(ArgumentParser.parse(new String[] {"-interval=0"}).isEmpty());
        Assert.assertTrue(ArgumentParser.parse(new String[] {"-interval=-5"}).isEmpty());
    }

    @Test
    public void nonNumericIntervalIsRejected() {
        Assert.assertTrue(ArgumentParser.parse(new String[] {"-interval=abc"}).isEmpty());
    }

    @Test
    public void unknownFlagIsRejected() {
        Assert.assertTrue(ArgumentParser.parse(new String[] {"--no-such-flag"}).isEmpty());
    }

    @Test
    public void colorFlagIsParsed() {
        JConsoleOptions o = ArgumentParser.parse(new String[] {"-c=#8BC6E1FF"}).orElseThrow();
        Assert.assertNotNull(o.color());
    }

    @Test
    public void bareHostPortBecomesJmxmpUrl() {
        JConsoleOptions o =
                ArgumentParser.parse(new String[] {"localhost:7091"}).orElseThrow();
        Assert.assertEquals(o.urls().size(), 1);
        Assert.assertEquals(o.urls().get(0), ArgumentParser.JMXMP_PREFIX + "localhost:7091");
    }

    @Test
    public void barePidTargetIsRejected() {
        // Bare pids (no colon) are no longer a supported connection target — the JMXMP-only
        // refactor removed local-attach. They must be rejected with an empty result.
        Assert.assertTrue(ArgumentParser.parse(new String[] {"1234"}).isEmpty());
    }

    @Test
    public void debugFlagWithUrlTargetCombines() {
        JConsoleOptions o =
                ArgumentParser.parse(new String[] {"-debug", "localhost:7091"}).orElseThrow();
        Assert.assertTrue(o.debug());
        Assert.assertEquals(o.urls().size(), 1);
    }
}
