/*
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

import javax.management.remote.JMXConnectorProvider;
import javax.management.remote.JMXConnectorServerProvider;

import com.sun.tools.jconsole.JConsolePlugin;

/**
 * Defines the JMX graphical tool, <em>{@index jconsole jconsole}</em>,
 * for monitoring and managing a running application.
 * <dl class="notes">
 * <dt>See Also:
 * <dd>{@extLink using_jconsole Using JConsole}
 * </dl>
 *
 * @toolGuide jconsole
 * @uses com.sun.tools.jconsole.JConsolePlugin
 * @moduleGraph
 * @since 9
 */
module com.druvu.jconsole {

	requires java.management.rmi;
	requires java.rmi;
	requires jdk.attach;
	requires jdk.internal.jvmstat;
	requires jdk.management;
	requires jdk.management.agent;

	requires transitive java.desktop;
	requires transitive java.management;
	requires jdk.jconsole;
	requires org.beryx.awt.color;

	exports com.druvu.jconsole.extra;

	uses JConsolePlugin;
	uses JMXConnectorServerProvider;
	uses JMXConnectorProvider;
}
