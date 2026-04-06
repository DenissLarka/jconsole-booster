package com.druvu.jconsole;

import com.druvu.jconsole.launcher.ArgumentParser;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Tests for {@link ArgumentParser} URL shorthand expansion. */
public class TestArgumentParser {

    @Test
    public void rmiShorthandExpandsToFullRmiUrl() {
        String result = ArgumentParser.adaptUrl("rmi:localhost:1234");
        Assert.assertEquals(result, "service:jmx:rmi:///jndi/rmi://localhost:1234/jmxrmi");
    }

    @Test
    public void rmiShorthandPreservesHostAndPort() {
        String result = ArgumentParser.adaptUrl("rmi:192.168.1.10:9999");
        Assert.assertEquals(result, "service:jmx:rmi:///jndi/rmi://192.168.1.10:9999/jmxrmi");
    }

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

    @Test
    public void fullRmiServiceUrlPassesThrough() {
        String url = "service:jmx:rmi:///jndi/rmi://remotehost:1099/jmxrmi";
        Assert.assertEquals(ArgumentParser.adaptUrl(url), url);
    }

    @Test
    public void rmiPrefixIsStrippedAndNotDoubled() {
        String result = ArgumentParser.adaptUrl("rmi:host:1099");
        Assert.assertFalse(result.contains("rmi:rmi:"), "rmi: prefix must not appear twice in: " + result);
    }
}
