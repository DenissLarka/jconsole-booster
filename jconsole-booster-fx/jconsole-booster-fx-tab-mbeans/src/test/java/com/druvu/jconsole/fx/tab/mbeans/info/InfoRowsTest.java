package com.druvu.jconsole.fx.tab.mbeans.info;

import static org.assertj.core.api.Assertions.assertThat;

import com.druvu.jconsole.fx.tab.mbeans.testing.TestServers;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import javax.management.JMException;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.ObjectName;
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
    public void anMBeanShowsItsNameClassThenItsDescriptor() {
        InfoTable table = InfoRows.forMBean(TestServers.GADGET, gadget);

        assertThat(table.title()).isEqualTo("MBeanInfo");
        assertThat(table.rows().getFirst()).isEqualTo(InfoRow.header("Info:"));
        assertThat(table.rows())
                .contains(
                        InfoRow.of("ObjectName", "test:type=Gadget,name=first"),
                        InfoRow.of("ClassName", "com.druvu.jconsole.fx.tab.mbeans.testing.Gadget"));
        assertThat(table.rows())
                .extracting(InfoRow::name)
                .containsSubsequence("ClassName", "Descriptor", "Info:", "interfaceClassName");
        assertThat(table.rows()).contains(InfoRow.section("Descriptor"));
    }

    @Test
    public void anAttributeShowsItsAccessAndType() {
        InfoTable table = InfoRows.forAttribute(Arrays.stream(gadget.getAttributes())
                .filter(attribute -> "Level".equals(attribute.getName()))
                .findFirst()
                .orElseThrow());

        assertThat(table.title()).isEqualTo("MBeanAttributeInfo");
        assertThat(table.rows())
                .contains(InfoRow.of("Readable", true), InfoRow.of("Writable", true), InfoRow.of("Type", "int"));
    }

    @Test
    public void anOperationShowsItsImpactAndParameters() {
        InfoTable table = InfoRows.forOperation(operation(gadget, "rename"));

        assertThat(table.rows())
                .contains(
                        InfoRow.of("ReturnType", "void"),
                        InfoRow.header("Parameter-0:"),
                        InfoRow.of("Type", "java.lang.String"));
        assertThat(InfoRows.impact(MBeanOperationInfo.ACTION_INFO)).isEqualTo("ACTION_INFO");
        assertThat(InfoRows.impact(42)).isEqualTo("UNKNOWN");
    }

    @Test
    public void anOperationWithoutDescriptorFieldsHasNoDescriptorSection() {
        InfoTable table = InfoRows.forOperation(operation(gadget, "rename"));

        assertThat(table.rows()).extracting(InfoRow::kind).doesNotContain(InfoRow.Kind.SECTION);
    }

    @Test
    public void anMxBeanOperationShowsItsDescriptorAfterItsProperties() throws JMException {
        MBeanInfo memory =
                ManagementFactory.getPlatformMBeanServer().getMBeanInfo(new ObjectName("java.lang:type=Memory"));

        InfoTable table = InfoRows.forOperation(operation(memory, "gc"));

        assertThat(table.rows())
                .containsSubsequence(
                        InfoRow.of("ReturnType", "void"),
                        InfoRow.section("Descriptor"),
                        InfoRow.header("Operation:"),
                        InfoRow.of("originalType", "void"));
    }

    @Test
    public void aNotificationShowsItsTypes() {
        InfoTable table = InfoRows.forNotification(gadget.getNotifications()[0]);

        assertThat(table.rows()).contains(InfoRow.of("NotifTypes", "[gadget.ping]"));
    }

    private static MBeanOperationInfo operation(MBeanInfo mbean, String name) {
        return Arrays.stream(mbean.getOperations())
                .filter(operation -> name.equals(operation.getName()))
                .findFirst()
                .orElseThrow();
    }
}
