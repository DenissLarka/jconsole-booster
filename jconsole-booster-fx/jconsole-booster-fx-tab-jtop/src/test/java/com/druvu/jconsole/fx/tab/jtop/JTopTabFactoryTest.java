package com.druvu.jconsole.fx.tab.jtop;

import static org.assertj.core.api.Assertions.assertThat;

import com.druvu.jconsole.core.vm.JmxDataAccess;
import com.druvu.jconsole.fx.tab.ConnectionTab;
import com.druvu.jconsole.fx.tab.TabContext;
import com.druvu.jconsole.fx.tab.TabHost;
import com.druvu.lib.loader.Dependencies;
import com.druvu.lib.loader.MultiComponentLoader;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import org.testng.annotations.Test;

/** The JTop tab found through druvu-lib-loader as the app finds it, and reading the connection it was given. */
public class JTopTabFactoryTest {

    @Test
    public void theLoaderBuildsTheTabFromTheConnection() throws IOException {
        JmxDataAccess localThreads =
                stub(JmxDataAccess.class, Map.of("getThreadMXBean", ManagementFactory.getThreadMXBean()));
        TabContext context = new TabContext(localThreads, stub(TabHost.class, Map.of()));

        List<ConnectionTab> tabs =
                MultiComponentLoader.loadAll(ConnectionTab.class, Dependencies.of(TabContext.class, context));

        assertThat(tabs).extracting(ConnectionTab::id).containsExactly("jtop");
        assertThat(tabs.getFirst().refresh()).isNotNull();
    }

    /** An implementation of {@code type} that gives the answers named and refuses every other call. */
    private static <T> T stub(Class<T> type, Map<String, Object> answers) {
        InvocationHandler handler = (proxy, method, args) -> {
            if (answers.containsKey(method.getName())) {
                return answers.get(method.getName());
            }
            throw new UnsupportedOperationException(method.getName());
        };
        return type.cast(
                Proxy.newProxyInstance(JTopTabFactoryTest.class.getClassLoader(), new Class<?>[] {type}, handler));
    }
}
