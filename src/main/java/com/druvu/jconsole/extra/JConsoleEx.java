package com.druvu.jconsole.extra;

import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.UIManager;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;

import org.beryx.awt.color.ColorFactory;

import com.druvu.jconsole.JConsole;

import lombok.extern.slf4j.Slf4j;

/**
 * @author Deniss Larka on 08 Jul 2022
 */
@Slf4j
public class JConsoleEx {

	private static final java.util.regex.Pattern PATTERN = Pattern.compile("^-c=(#\\p{XDigit}+)$");
	public static final String JMXMP_PREFIX = "service:jmx:jmxmp://";
	public static final String RMI_PREFIX = "service:jmx:rmi:///jndi/rmi://";

	public static void main(String[] args) {

		// Set cross-platform Java L&F (also called "Metal")
		setLookAndFeel();
		List<String> newArgs = new ArrayList<>();
		if (args.length > 0) {
			for (int i = 0; i < args.length; i++) {
				final Matcher matcher = PATTERN.matcher(args[i]);
				if (matcher.matches()) {
					colorEnhancement(matcher.group(1));
					continue;
				}
				if (args[i].indexOf(':') != -1) {
					newArgs.add(possiblyAdaptUrl(args[i]));
					continue;
				}
				newArgs.add(args[i]);
			}
		}
		JConsole.main(newArgs.toArray(new String[0]));
	}

	private static void setLookAndFeel() {
		try {
			final NimbusLookAndFeel feel = new NimbusLookAndFeel();
			UIManager.setLookAndFeel(feel);
		} catch (Exception e) {
			log.error("Cannot set cross-platform L&F", e);
		}
	}

	public static String possiblyAdaptUrl(String arg) {
		// Already a complete JMX service URL of any protocol — pass through unchanged
		if (arg.startsWith("service:jmx:")) {
			return arg;
		}
		// "rmi:host:port" shorthand → full RMI service URL
		if (arg.startsWith("rmi:")) {
			return RMI_PREFIX + arg.substring(4) + "/jmxrmi";
		}
		// "host:port" bare shorthand → default to JMXMP
		return JMXMP_PREFIX + arg;
	}

	// https://dzone.com/articles/create-javaawtcolor-from-string-representation
	// https://docs.oracle.com/javase/tutorial/uiswing/lookandfeel/_nimbusDefaults.html#primary
	// UIManager.put("nimbusBase", new Color(191,98,4));
	private static void colorEnhancement(String colorStr) {
		Color color = ColorFactory.valueOf(colorStr);
		UIManager.put("nimbusBlueGrey", color);
	}
}
