package com.druvu.jconsole.fx.tab.mbeans;

import com.druvu.jconsole.core.text.Messages;
import com.druvu.jconsole.fx.tab.ConnectionTab;
import com.druvu.jconsole.fx.tab.TabContext;
import com.druvu.jconsole.fx.tab.mbeans.common.MBeanAccess;
import com.druvu.jconsole.fx.tab.mbeans.common.TabServices;
import com.druvu.jconsole.fx.tab.mbeans.notifications.NotificationBuffer;
import com.druvu.jconsole.fx.tab.mbeans.operations.OperationMemory;
import java.io.IOException;
import javafx.scene.Node;

/**
 * The MBeans tab: the MBean tree and the views of its nodes. Its content is built on first show; disposing it removes
 * its listeners from the server.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
final class MBeansTab implements ConnectionTab {

    private static final Runnable NOTHING_TO_SHOW = () -> {};

    private final TabContext context;
    private final OperationMemory memory;
    private final TabServices services;
    private final NotificationBuffer buffer;
    /** Built on the FX thread at the first {@link #content()}. */
    private MBeansView view;

    /** @param memory the remembered operation parameter values */
    MBeansTab(TabContext context, OperationMemory memory) {
        this.context = context;
        this.memory = memory;
        MBeanAccess access = new MBeanAccess(context.connection().getMBeanServerConnection());
        this.services = new TabServices(access, context.host());
        this.buffer = new NotificationBuffer(access.connection());
    }

    @Override
    public String id() {
        return "mbeans";
    }

    @Override
    public String title() {
        return Messages.MBEANS;
    }

    @Override
    public int place() {
        return 70;
    }

    @Override
    public Node content() {
        MBeansView shown = view;
        if (shown == null) {
            shown = new MBeansView(services, memory, buffer);
            view = shown;
            shown.load();
        }
        return shown.node();
    }

    /** Only checks the connection is alive; the attribute values refresh on their Refresh button. */
    @Override
    public Runnable refresh() throws IOException {
        context.connection().getMBeanServerConnection().getDefaultDomain();
        return NOTHING_TO_SHOW;
    }

    /** Stops the tab's threads, then removes its listeners from the server in the background. */
    void dispose() {
        services.close();
        MBeansView shown = view;
        Thread.ofVirtual().name("mbeans-tab-dispose").start(() -> {
            if (shown != null) {
                shown.stopListening();
            }
            buffer.unsubscribeAll();
        });
    }
}
