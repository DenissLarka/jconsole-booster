package com.druvu.jconsole.fx.tab.mbeans.common;

import static com.druvu.jconsole.fx.tab.mbeans.testing.TestServers.GADGET;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.druvu.jconsole.fx.tab.mbeans.testing.TestServers;
import java.io.IOException;
import java.util.concurrent.CompletionException;
import javax.management.Attribute;
import javax.management.InstanceNotFoundException;
import javax.management.JMException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import org.testng.annotations.Test;

/** The tab's calls to a real MBean server, and the failure the user sees behind the wrappers. */
public class MBeanAccessTest {

    @Test
    public void theInfoIsReadOnceUntilForgotten() throws JMException, IOException {
        MBeanServer server = TestServers.withGadget();
        MBeanAccess access = new MBeanAccess(server);

        assertThat(access.info(GADGET)).isSameAs(access.info(GADGET));

        server.unregisterMBean(GADGET);
        assertThat(access.info(GADGET)).as("still kept").isNotNull();

        access.forget(GADGET);
        assertThatThrownBy(() -> access.info(GADGET)).isInstanceOf(InstanceNotFoundException.class);
    }

    @Test
    public void theMetadataTellsABroadcaster() throws JMException, IOException {
        MBeanAccess access = new MBeanAccess(TestServers.withGadget());

        assertThat(access.metadata(GADGET).broadcaster()).isTrue();
        assertThat(access.isBroadcaster(TestServers.name("JMImplementation:type=MBeanServerDelegate")))
                .isTrue();
        assertThat(access.isBroadcaster(TestServers.name("test:type=Missing"))).isFalse();
    }

    @Test
    public void writesAndInvokesGoThroughTheConnection() throws JMException, IOException {
        MBeanAccess access = new MBeanAccess(TestServers.withGadget());

        access.writeAttribute(GADGET, new Attribute("Level", 7));
        assertThat(access.readAttribute(GADGET, "Level")).isEqualTo(7);
        assertThat(access.invoke(GADGET, "echo", new Object[] {"hi"}, new String[] {String.class.getName()}))
                .isEqualTo("hi");
    }

    @Test
    public void theFailureShownIsTheMBeansOwn() {
        IllegalStateException own = new IllegalStateException("own");

        assertThat(Failures.cause(new CompletionException(new MBeanException(own))))
                .isSameAs(own);
        assertThat(Failures.cause(own)).isSameAs(own);
    }
}
