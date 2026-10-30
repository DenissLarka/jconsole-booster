package com.druvu.jconsole.fx.tab.summary;

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

/** Covers how the app finds the tab: registered with druvu-lib-loader, created with its context, disposed again. */
public class SummaryTabFactoryTest {

    @Test
    public void theLoaderFindsTheSummaryTab() {
        List<ConnectionTab> tabs = MultiComponentLoader.loadAll(ConnectionTab.class, dependencies());
        try {
            assertThat(tabs).extracting(ConnectionTab::id).containsExactly("summary");
            ConnectionTab summary = tabs.getFirst();
            assertThat(summary.title()).isEqualTo("VM Summary");
            assertThat(summary.place()).isEqualTo(50);
        } finally {
            assertThat(MultiComponentLoader.disposeAll(ConnectionTab.class, tabs))
                    .hasSameSizeAs(tabs);
        }
    }

    @Test
    public void aRefreshReturnsTheStepThatShowsTheReading() throws IOException {
        ConnectionTab tab = new SummaryTabFactory().createComponent(dependencies());

        assertThat(tab.refresh()).isNotNull();
    }

    private static Dependencies dependencies() {
        TabContext context = new TabContext(new LocalJvmConnection(true), new SilentHost());
        return Dependencies.of(TabContext.class, context);
    }

    /** An app that has no window and says nothing. */
    private static final class SilentHost implements TabHost {

        @Override
        public Window window() {
            return null;
        }

        @Override
        public void showError(String title, Throwable failure) {
            // nothing to show it in
        }

        @Override
        public void notice(String message) {
            // nothing to show it in
        }
    }
}
