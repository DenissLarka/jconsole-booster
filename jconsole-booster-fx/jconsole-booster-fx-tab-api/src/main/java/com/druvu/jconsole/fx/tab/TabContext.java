package com.druvu.jconsole.fx.tab;

import com.druvu.jconsole.core.vm.JmxDataAccess;
import java.util.Objects;

/**
 * What a tab receives at creation: the live connection it reads, and the app it lives in.
 *
 * @param connection the connection to the target JVM, shared by every tab
 * @param host the app's services: its window, dialogs and notices
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
public record TabContext(JmxDataAccess connection, TabHost host) {

    public TabContext {
        Objects.requireNonNull(connection, "connection");
        Objects.requireNonNull(host, "host");
    }
}
