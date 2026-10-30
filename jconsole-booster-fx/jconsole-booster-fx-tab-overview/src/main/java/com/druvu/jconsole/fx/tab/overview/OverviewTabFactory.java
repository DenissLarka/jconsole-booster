package com.druvu.jconsole.fx.tab.overview;

import com.druvu.jconsole.fx.tab.ConnectionTab;
import com.druvu.jconsole.fx.tab.TabContext;
import com.druvu.lib.loader.ComponentFactory;
import com.druvu.lib.loader.Dependencies;

/**
 * Registers the Overview tab with druvu-lib-loader, which is how the app finds it at run time.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
public final class OverviewTabFactory implements ComponentFactory<ConnectionTab> {

    @Override
    public ConnectionTab createComponent(Dependencies dependencies) {
        TabContext context = dependencies.getDependency(TabContext.class);
        return new OverviewTab(new OverviewReader(context.connection()));
    }

    @Override
    public Class<ConnectionTab> type() {
        return ConnectionTab.class;
    }
}
