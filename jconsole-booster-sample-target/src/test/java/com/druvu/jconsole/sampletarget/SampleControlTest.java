package com.druvu.jconsole.sampletarget;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import javax.management.JMException;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import org.testng.annotations.Test;

/** The log level operation: its descriptor fields reach the MBeanInfo, and it sets only the levels it names. */
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
    public void changeLogLevelRefusesAnUnknownLevel() {
        assertThatThrownBy(() -> new SampleControl().changeLogLevel("LOUD"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("LOUD");
    }
}
