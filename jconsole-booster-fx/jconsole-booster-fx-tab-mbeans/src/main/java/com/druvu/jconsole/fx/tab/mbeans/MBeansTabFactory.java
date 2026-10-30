package com.druvu.jconsole.fx.tab.mbeans;

import com.druvu.jconsole.core.operations.OperationStateStore;
import com.druvu.jconsole.fx.tab.ConnectionTab;
import com.druvu.jconsole.fx.tab.TabContext;
import com.druvu.jconsole.fx.tab.mbeans.operations.StoredOperationMemory;
import com.druvu.lib.loader.ComponentFactory;
import com.druvu.lib.loader.Dependencies;

/**
 * Registers the MBeans tab with druvu-lib-loader, which is how the app finds it at connect. Disposing the tab removes
 * its listeners from the server.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
public final class MBeansTabFactory implements ComponentFactory<ConnectionTab> {

    @Override
    public ConnectionTab createComponent(Dependencies dependencies) {
        TabContext context = dependencies.getDependency(TabContext.class);
        return new MBeansTab(context, new StoredOperationMemory(new OperationStateStore()));
    }

    @Override
    public ConnectionTab disposeComponent(ConnectionTab tab) {
        if (tab instanceof MBeansTab mbeans) {
            mbeans.dispose();
        }
        return tab;
    }

    @Override
    public Class<ConnectionTab> type() {
        return ConnectionTab.class;
    }
}
