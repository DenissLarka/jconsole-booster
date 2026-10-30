package com.druvu.jconsole.fx.tab.overview;

import static org.assertj.core.api.Assertions.assertThat;

import com.druvu.jconsole.fx.tab.ConnectionTab;
import com.druvu.jconsole.fx.tab.TabContext;
import com.druvu.jconsole.fx.tab.TabHost;
import com.druvu.lib.loader.Dependencies;
import com.druvu.lib.loader.MultiComponentLoader;
import java.io.IOException;
import java.util.List;
import javafx.stage.Window;
import org.testng.annotations.Test;

/** The Overview tab found through druvu-lib-loader as the app finds it, and reading the connection it was given. */
public class OverviewTabFactoryTest {

    @Test
    public void theLoaderBuildsTheTabFromTheConnection() throws IOException {
        TabContext context = new TabContext(new LocalJvmConnection(), new SilentHost());

        List<ConnectionTab> tabs =
                MultiComponentLoader.loadAll(ConnectionTab.class, Dependencies.of(TabContext.class, context));

        assertThat(tabs).extracting(ConnectionTab::id).containsExactly("overview");
        assertThat(tabs.getFirst().refresh()).isNotNull();
    }

    /** A host the Overview never calls. */
    private static final class SilentHost implements TabHost {

        @Override
        public Window window() {
            return null;
        }

        @Override
        public void showError(String title, Throwable failure) {
            // the Overview reports no errors
        }

        @Override
        public void notice(String message) {
            // the Overview shows no notices
        }
    }
}
