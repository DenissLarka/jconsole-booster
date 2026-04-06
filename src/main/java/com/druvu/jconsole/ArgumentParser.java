package com.druvu.jconsole;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.beryx.awt.color.ColorFactory;

import lombok.extern.slf4j.Slf4j;

/**
 * Parses command-line arguments into {@link JConsoleOptions}.
 *
 * <p>
 * URL shorthands are expanded before classification:
 * <ul>
 * <li>{@code service:jmx:…} — passed through unchanged</li>
 * <li>{@code rmi:host:port} →
 * {@code service:jmx:rmi:///jndi/rmi://host:port/jmxrmi}</li>
 * <li>{@code host:port} → {@code service:jmx:jmxmp://host:port}</li>
 * </ul>
 */
@Slf4j
public class ArgumentParser {

	private static final Pattern COLOR_PATTERN = Pattern.compile("^-c=(#\\p{XDigit}+)$");

	static final String JMXMP_PREFIX = "service:jmx:jmxmp://";
	static final String RMI_PREFIX = "service:jmx:rmi:///jndi/rmi://";

	/**
	 * Parses {@code args} and returns launch options. Returns
	 * {@link Optional#empty()} when the application should exit immediately (usage
	 * or version was printed, or a fatal argument error occurred).
	 */
	public static Optional<JConsoleOptions> parse(String[] args) {
		int updateInterval = 4000;
		String pluginPath = "";
		boolean noTile = false;
		boolean debug = false;
		Color color = null;

		int argIndex = 0;

		// --- flag arguments (all start with '-') ---
		while (args.length - argIndex > 0 && args[argIndex].startsWith("-")) {
			String arg = args[argIndex++];

			if (arg.equals("-h") || arg.equals("-help") || arg.equals("-?")) {
				usage();
				return Optional.empty();
			}

			Matcher colorMatcher = COLOR_PATTERN.matcher(arg);
			if (colorMatcher.matches()) {
				color = ColorFactory.valueOf(colorMatcher.group(1));
				continue;
			}

			if (arg.startsWith("-interval=")) {
				try {
					updateInterval = Integer.parseInt(arg.substring(10)) * 1000;
					if (updateInterval <= 0) {
						usage();
						return Optional.empty();
					}
				} catch (NumberFormatException ex) {
					usage();
					return Optional.empty();
				}
			} else if (arg.equals("-pluginpath")) {
				if (argIndex < args.length && !args[argIndex].startsWith("-")) {
					pluginPath = args[argIndex++];
				} else {
					usage();
					return Optional.empty();
				}
			} else if (arg.equals("-notile")) {
				noTile = true;
			} else if (arg.equals("-version")) {
				Version.print(System.err);
				return Optional.empty();
			} else if (arg.equals("-fullversion")) {
				Version.printFullVersion(System.err);
				return Optional.empty();
			} else if (arg.equals("-debug")) {
				debug = true;
			} else {
				usage();
				return Optional.empty();
			}
		}

		boolean hotspot = System.getProperty("jconsole.showUnsupported") != null;

		// --- connection targets ---
		List<String> urls = new ArrayList<>();
		List<LocalVirtualMachine> vmids = new ArrayList<>();

		for (int i = argIndex; i < args.length; i++) {
			String arg = args[i];

			if (arg.indexOf(':') != -1) {
				// URL-like: expand shorthands then record as a full service URL
				urls.add(adaptUrl(arg));
			} else {
				// Treat as a local PID
				if (!JConsole.isLocalAttachAvailable()) {
					log.error("Local process monitoring is not supported");
					return Optional.empty();
				}
				try {
					int vmid = Integer.parseInt(arg);
					LocalVirtualMachine lvm = LocalVirtualMachine.getLocalVirtualMachine(vmid);
					if (lvm == null) {
						log.error("Invalid process id: {}", vmid);
						return Optional.empty();
					}
					vmids.add(lvm);
				} catch (NumberFormatException ex) {
					usage();
					return Optional.empty();
				}
			}
		}

		return Optional.of(new JConsoleOptions(noTile, hotspot, debug, updateInterval, pluginPath, color, urls, vmids));
	}

	/**
	 * Expands JMX URL shorthands to full {@code service:jmx:} service URLs. Full
	 * service URLs are returned unchanged.
	 */
	static String adaptUrl(String arg) {
		if (arg.startsWith("service:jmx:")) {
			return arg;
		}
		if (arg.startsWith("rmi:")) {
			return RMI_PREFIX + arg.substring(4) + "/jmxrmi";
		}
		// bare host:port → JMXMP
		return JMXMP_PREFIX + arg;
	}

	private static void usage() {
		System.out.println(Resources.format(Messages.ZZ_USAGE_TEXT, "jconsole"));
	}
}
