/*
 * Copyright (c) 2004, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.druvu.jconsole.core.mbeans;

import java.util.concurrent.ExecutionException;
import javax.management.MBeanException;
import javax.management.ReflectionException;
import javax.management.RuntimeMBeanException;
import javax.management.RuntimeOperationsException;

/** Unwraps what an MBean call threw, so the user sees the target's own exception rather than JMX's wrapper. */
public final class MBeanExceptions {

    private MBeanExceptions() {}

    /**
     * The exception behind {@code e}: the cause of an {@link ExecutionException}, then the cause of JMX's
     * {@link MBeanException}, {@link RuntimeMBeanException}, {@link RuntimeOperationsException} or
     * {@link ReflectionException}; {@code e} itself when nothing wraps it.
     */
    public static Throwable actualCause(Throwable e) {
        if (e instanceof ExecutionException) {
            e = e.getCause();
        }
        if (e instanceof MBeanException
                || e instanceof RuntimeMBeanException
                || e instanceof RuntimeOperationsException
                || e instanceof ReflectionException) {
            Throwable t = e.getCause();
            if (t != null) {
                return t;
            }
        }
        return e;
    }
}
