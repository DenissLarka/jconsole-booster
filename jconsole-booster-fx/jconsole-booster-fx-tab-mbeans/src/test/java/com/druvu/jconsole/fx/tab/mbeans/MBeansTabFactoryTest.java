package com.druvu.jconsole.fx.tab.mbeans;

import static org.assertj.core.api.Assertions.assertThat;

import com.druvu.jconsole.fx.tab.ConnectionTab;
import com.druvu.lib.loader.ComponentFactory;
import java.util.ServiceLoader;
import org.testng.annotations.Test;

/** The app finds the tab's factory through the service registration. */
public class MBeansTabFactoryTest {

    @Test
    public void theFactoryIsRegisteredForConnectionTabs() {
        assertThat(ServiceLoader.load(ComponentFactory.class).stream())
                .anySatisfy(provider -> assertThat(provider.type()).isEqualTo(MBeansTabFactory.class));
        assertThat(new MBeansTabFactory().type()).isEqualTo(ConnectionTab.class);
    }
}
