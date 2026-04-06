package com.druvu.jconsole.extra;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Tests for {@link JConsoleEx} logic that has no UI dependency.
 *
 * @author Deniss Larka
 */
public class TestJConsoleEx {

	// -------------------------------------------------------------------------
	// possiblyAdaptUrl
	// -------------------------------------------------------------------------

	@Test
	public void rmiShorthandExpandsToFullRmiUrl() {
		String result = JConsoleEx.possiblyAdaptUrl("rmi:localhost:1234");
		Assert.assertEquals(result, "service:jmx:rmi:///jndi/rmi://localhost:1234/jmxrmi");
	}

	@Test
	public void rmiShorthandPreservesHostAndPort() {
		String result = JConsoleEx.possiblyAdaptUrl("rmi:192.168.1.10:9999");
		Assert.assertEquals(result, "service:jmx:rmi:///jndi/rmi://192.168.1.10:9999/jmxrmi");
	}

	@Test
	public void bareHostColonPortDefaultsToJmxmp() {
		String result = JConsoleEx.possiblyAdaptUrl("localhost:7091");
		Assert.assertEquals(result, JConsoleEx.JMXMP_PREFIX + "localhost:7091");
	}

	@Test
	public void fullJmxmpUrlPassesThrough() {
		String url = "service:jmx:jmxmp://remotehost:5000";
		Assert.assertEquals(JConsoleEx.possiblyAdaptUrl(url), url);
	}

	@Test
	public void fullRmiServiceUrlPassesThrough() {
		String url = "service:jmx:rmi:///jndi/rmi://remotehost:1099/jmxrmi";
		Assert.assertEquals(JConsoleEx.possiblyAdaptUrl(url), url);
	}

	@Test
	public void rmiPrefixIsStrippedAndNotDoubled() {
		// Ensure "rmi:" prefix is removed before prepending the full RMI URL
		String result = JConsoleEx.possiblyAdaptUrl("rmi:host:1099");
		Assert.assertFalse(result.contains("rmi:rmi:"), "rmi: prefix must not appear twice in: " + result);
	}
}
