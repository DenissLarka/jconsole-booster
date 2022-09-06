package com.druvu.jconsole.extra;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.UIManager;

import org.beryx.awt.color.ColorFactory;

import com.druvu.jconsole.JConsole;

/**
 * @author Deniss Larka
 * on 08 Jul 2022
 */
public class JConsoleEx {

	private static final java.util.regex.Pattern PATTERN = Pattern.compile("^-c=(#\\p{XDigit}+)$");
	public static final String JMXMP_PREFIX = "service:jmx:jmxmp://";
	public static final String RMI_PREFIX = "service:jmx:rmi:///jndi/rmi://";

	public static void main(String[] args) {
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

	public static String possiblyAdaptUrl(String arg) {
		//making easier to use rmi proto
		if (arg.startsWith("rmi:")) {
			return RMI_PREFIX + arg.substring(4) + "/jmxrmi";
		}
		//by default considering jmxmp for a simple case like host:port
		if (!arg.startsWith(JMXMP_PREFIX)) {
			return JMXMP_PREFIX + arg;
		}
		return arg;
	}

	//https://dzone.com/articles/create-javaawtcolor-from-string-representation
	//https://docs.oracle.com/javase/tutorial/uiswing/lookandfeel/_nimbusDefaults.html#primary
	//UIManager.put("nimbusBase", new Color(191,98,4));
	private static void colorEnhancement(String colorStr) {
		Color color = ColorFactory.valueOf(colorStr);
		UIManager.put("nimbusBlueGrey", color);
	}
}
