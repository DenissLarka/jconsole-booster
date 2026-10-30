package com.druvu.jconsole.fx.tab.mbeans.notifications;

import static com.druvu.jconsole.fx.tab.mbeans.testing.TestServers.GADGET;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.druvu.jconsole.fx.tab.mbeans.testing.Gadget;
import com.druvu.jconsole.fx.tab.mbeans.testing.TestServers;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.management.InstanceNotFoundException;
import javax.management.JMException;
import javax.management.MBeanServer;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** The notification buffer on a real MBean server: newest first, counted, kept after unsubscribing. */
public class NotificationBufferTest {

    private final List<String> told = new CopyOnWriteArrayList<>();
    private MBeanServer server;
    private NotificationBuffer buffer;

    @BeforeMethod
    public void newBuffer() {
        server = TestServers.withGadget();
        buffer = new NotificationBuffer(server);
        told.clear();
        buffer.onReceived((name, row) -> told.add(row.message()));
    }

    @Test
    public void notificationsArriveNewestFirstAndCounted() throws JMException, IOException {
        buffer.subscribe(GADGET);
        ping("one");
        ping("two");

        assertThat(buffer.isSubscribed(GADGET)).isTrue();
        assertThat(buffer.received(GADGET)).isEqualTo(2);
        assertThat(buffer.rows(GADGET)).extracting(NotificationRow::message).containsExactly("two", "one");
        assertThat(buffer.rows(GADGET).getFirst().type()).isEqualTo(Gadget.PING);
        assertThat(buffer.rows(GADGET).getFirst().sequenceNumber()).isEqualTo(2);
        assertThat(told).containsExactly("one", "two");
    }

    @Test
    public void nothingIsKeptBeforeSubscribing() throws JMException {
        ping("lost");

        assertThat(buffer.rows(GADGET)).isEmpty();
        assertThat(buffer.received(GADGET)).isZero();
        assertThat(told).isEmpty();
    }

    @Test
    public void unsubscribingKeepsTheBufferAndSubscribingAgainEmptiesIt() throws JMException, IOException {
        buffer.subscribe(GADGET);
        ping("kept");
        buffer.unsubscribe(GADGET);
        ping("ignored");

        assertThat(buffer.isSubscribed(GADGET)).isFalse();
        assertThat(buffer.rows(GADGET)).extracting(NotificationRow::message).containsExactly("kept");

        buffer.subscribe(GADGET);

        assertThat(buffer.rows(GADGET)).isEmpty();
    }

    @Test
    public void clearingStartsTheCountAgain() throws JMException, IOException {
        buffer.subscribe(GADGET);
        ping("one");
        buffer.clear(GADGET);

        assertThat(buffer.rows(GADGET)).isEmpty();
        assertThat(buffer.received(GADGET)).isZero();

        ping("two");

        assertThat(buffer.received(GADGET)).isEqualTo(1);
    }

    @Test
    public void aGoneMBeanIsForgotten() throws JMException, IOException {
        buffer.subscribe(GADGET);
        ping("one");
        buffer.forget(GADGET);

        assertThat(buffer.isSubscribed(GADGET)).isFalse();
        assertThat(buffer.rows(GADGET)).isEmpty();
    }

    @Test
    public void aMissingMBeanCannotBeSubscribedTo() {
        assertThatThrownBy(() -> buffer.subscribe(TestServers.name("test:type=Missing")))
                .isInstanceOf(InstanceNotFoundException.class);
        assertThat(buffer.isSubscribed(TestServers.name("test:type=Missing"))).isFalse();
    }

    @Test
    public void unsubscribingFromAllStopsEveryListener() throws JMException, IOException {
        buffer.subscribe(GADGET);
        buffer.unsubscribeAll();
        ping("after");

        assertThat(told).isEmpty();
    }

    private void ping(String message) throws JMException {
        server.invoke(GADGET, "ping", new Object[] {message}, new String[] {String.class.getName()});
    }
}
