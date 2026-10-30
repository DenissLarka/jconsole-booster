package com.druvu.jconsole.fx.tab.mbeans.info;

import static org.assertj.core.api.Assertions.assertThat;

import com.druvu.jconsole.fx.tab.mbeans.testing.TestServers;
import java.util.Arrays;
import javax.management.JMException;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/** The info rows of an MBean and its features, read from a real MBean server. */
public class InfoRowsTest {

    private MBeanInfo gadget;

    @BeforeClass
    public void readTheGadget() throws JMException {
        gadget = TestServers.withGadget().getMBeanInfo(TestServers.GADGET);
    }

    @Test
    public void anMBeanShowsItsNameClassAndDescriptor() {
        InfoTables tables = InfoRows.forMBean(TestServers.GADGET, gadget);

        assertThat(tables.title()).isEqualTo("MBeanInfo");
        assertThat(tables.info().getFirst()).isEqualTo(new InfoRow("Info:", "", true));
        assertThat(tables.info())
                .contains(
                        new InfoRow("ObjectName", "test:type=Gadget,name=first", false),
                        new InfoRow("ClassName", "com.druvu.jconsole.fx.tab.mbeans.testing.Gadget", false));
        assertThat(tables.descriptor().getFirst()).isEqualTo(new InfoRow("Info:", "", true));
        assertThat(tables.descriptor()).extracting(InfoRow::name).contains("interfaceClassName");
    }

    @Test
    public void anAttributeShowsItsAccessAndType() {
        InfoTables tables = InfoRows.forAttribute(Arrays.stream(gadget.getAttributes())
                .filter(attribute -> "Level".equals(attribute.getName()))
                .findFirst()
                .orElseThrow());

        assertThat(tables.title()).isEqualTo("MBeanAttributeInfo");
        assertThat(tables.info())
                .contains(
                        new InfoRow("Readable", "true", false),
                        new InfoRow("Writable", "true", false),
                        new InfoRow("Type", "int", false));
    }

    @Test
    public void anOperationShowsItsImpactAndParameters() {
        MBeanOperationInfo rename = Arrays.stream(gadget.getOperations())
                .filter(operation -> "rename".equals(operation.getName()))
                .findFirst()
                .orElseThrow();

        InfoTables tables = InfoRows.forOperation(rename);

        assertThat(tables.info())
                .contains(
                        new InfoRow("ReturnType", "void", false),
                        new InfoRow("Parameter-0:", "", true),
                        new InfoRow("Type", "java.lang.String", false));
        assertThat(InfoRows.impact(MBeanOperationInfo.ACTION_INFO)).isEqualTo("ACTION_INFO");
        assertThat(InfoRows.impact(42)).isEqualTo("UNKNOWN");
    }

    @Test
    public void aNotificationShowsItsTypes() {
        InfoTables tables = InfoRows.forNotification(gadget.getNotifications()[0]);

        assertThat(tables.info()).contains(new InfoRow("NotifTypes", "[gadget.ping]", false));
    }
}
