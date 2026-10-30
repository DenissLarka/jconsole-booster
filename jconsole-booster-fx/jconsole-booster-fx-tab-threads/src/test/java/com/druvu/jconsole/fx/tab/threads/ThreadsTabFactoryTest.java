package com.druvu.jconsole.fx.tab.threads;

import static org.assertj.core.api.Assertions.assertThat;

import com.druvu.jconsole.fx.tab.ConnectionTab;
import com.druvu.jconsole.fx.tab.TabContext;
import com.druvu.lib.loader.Dependencies;
import com.druvu.lib.loader.MultiComponentLoader;
import java.io.IOException;
import java.util.List;
import org.testng.annotations.Test;

/** The Threads tab found through druvu-lib-loader as the app finds it, and reading the connection it was given. */
public class ThreadsTabFactoryTest {

    @Test
    public void theLoaderBuildsTheTabFromTheConnection() throws IOException {
        TabContext context = new TabContext(new LocalJvmConnection(), new RecordingHost());

        List<ConnectionTab> tabs =
                MultiComponentLoader.loadAll(ConnectionTab.class, Dependencies.of(TabContext.class, context));

        assertThat(tabs).extracting(ConnectionTab::id).containsExactly("threads");
        assertThat(tabs.getFirst().refresh()).isNotNull();
    }
}
