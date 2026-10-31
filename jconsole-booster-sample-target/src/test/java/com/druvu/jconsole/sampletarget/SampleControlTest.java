package com.druvu.jconsole.sampletarget;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.management.AttributeChangeNotification;
import javax.management.JMException;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.Notification;
import javax.management.ObjectName;
import org.testng.annotations.Test;

/**
 * The control MBean: the log level operation's descriptor fields reach the MBeanInfo and it sets only the levels it
 * names; the notifications are declared and reach a listener with the MBean's name as their source.
 */
public class SampleControlTest {

    @Test
    public void changeLogLevelCarriesItsDescriptorFields() throws JMException {
        MBeanServer server = MBeanServerFactory.newMBeanServer();
        ObjectName name = new ObjectName(SampleTargetJvm.OBJECT_NAME);
        server.registerMBean(new SampleControl(), name);

        MBeanInfo info = server.getMBeanInfo(name);
        MBeanOperationInfo operation = Arrays.stream(info.getOperations())
                .filter(op -> op.getName().equals("changeLogLevel"))
                .findFirst()
                .orElseThrow();

        assertThat(operation.getDescriptor().getFieldValue("since")).isEqualTo("1.2");
        assertThat(operation.getSignature()[0].getDescriptor().getFieldValue("legalValues"))
                .isEqualTo(new String[] {"ERROR", "WARN", "INFO", "DEBUG"});
        assertThat(operation.getSignature()[0].getDescriptor().getFieldValue("defaultValue"))
                .isEqualTo("INFO");
    }

    @Test
    public void changeLogLevelReturnsThePreviousLevel() {
        SampleControl control = new SampleControl();

        assertThat(control.changeLogLevel("debug")).isEqualTo("INFO");
        assertThat(control.changeLogLevel("WARN")).isEqualTo("DEBUG");
    }

    @Test
    public void theNotificationsAreDeclared() throws JMException {
        MBeanServer server = MBeanServerFactory.newMBeanServer();
        ObjectName name = new ObjectName(SampleTargetJvm.OBJECT_NAME);
        server.registerMBean(new SampleControl(), name);

        assertThat(Arrays.stream(server.getMBeanInfo(name).getNotifications())
                        .flatMap(info -> Arrays.stream(info.getNotifTypes())))
                .containsExactly(
                        AttributeChangeNotification.ATTRIBUTE_CHANGE, SampleControl.HEARTBEAT, SampleControl.MESSAGE);
        assertThat(server.getMBeanInfo(name).getNotifications())
                .extracting(MBeanNotificationInfo::getName)
                .contains(AttributeChangeNotification.class.getName());
    }

    @Test
    public void aLogLevelChangeSendsAnAttributeChange() throws JMException {
        MBeanServer server = MBeanServerFactory.newMBeanServer();
        ObjectName name = new ObjectName(SampleTargetJvm.OBJECT_NAME);
        server.registerMBean(new SampleControl(), name);
        List<Notification> received = new ArrayList<>();
        server.addNotificationListener(name, (notification, handback) -> received.add(notification), null, null);

        server.invoke(name, "changeLogLevel", new Object[] {"DEBUG"}, new String[] {String.class.getName()});
        server.invoke(name, "changeLogLevel", new Object[] {"DEBUG"}, new String[] {String.class.getName()});

        assertThat(received).singleElement().isInstanceOfSatisfying(AttributeChangeNotification.class, change -> {
            assertThat(change.getSource()).isEqualTo(name);
            assertThat(change.getAttributeName()).isEqualTo("LogLevel");
            assertThat(change.getOldValue()).isEqualTo("INFO");
            assertThat(change.getNewValue()).isEqualTo("DEBUG");
        });
        assertThat(server.getAttribute(name, "LogLevel")).isEqualTo("DEBUG");
    }

    @Test
    public void aBroadcastAndAHeartbeatReachTheListener() {
        SampleControl control = new SampleControl();
        List<Notification> received = new ArrayList<>();
        control.addNotificationListener((notification, handback) -> received.add(notification), null, null);
        control.recordRequest();

        control.broadcast("hello");
        control.sendHeartbeat();

        assertThat(received)
                .extracting(Notification::getType, Notification::getMessage)
                .containsExactly(tuple(SampleControl.MESSAGE, "hello"), tuple(SampleControl.HEARTBEAT, "1 requests"));
        assertThat(received.get(1).getUserData()).isEqualTo(1L);
        assertThat(received.get(1).getSequenceNumber())
                .isGreaterThan(received.get(0).getSequenceNumber());
    }

    @Test
    public void changeLogLevelRefusesAnUnknownLevel() {
        assertThatThrownBy(() -> new SampleControl().changeLogLevel("LOUD"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("LOUD");
    }
}
