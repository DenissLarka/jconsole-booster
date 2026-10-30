package com.druvu.jconsole.fx.tab.mbeans.attributes;

import static org.assertj.core.api.Assertions.assertThat;

import com.druvu.jconsole.fx.tab.mbeans.common.MBeanAccess;
import com.druvu.jconsole.fx.tab.mbeans.testing.TestServers;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.management.JMException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/** Attribute values read from a real MBean server: a failing getter costs only its own row. */
public class AttributeReaderTest {

    private Map<String, AttributeValue> rows;

    @BeforeClass
    public void readTheGadget() throws JMException, IOException {
        MBeanAccess access = new MBeanAccess(TestServers.withGadget());
        List<AttributeValue> values = AttributeReader.read(
                access,
                TestServers.GADGET,
                List.of(access.info(TestServers.GADGET).getAttributes()));
        rows = values.stream().collect(Collectors.toMap(AttributeValue::name, Function.identity()));
    }

    @Test
    public void theGoodAttributesAreRead() {
        assertThat(rows.get("Level").value()).isEqualTo(3);
        assertThat(rows.get("Name").text()).isEqualTo("Alpha");
    }

    @Test
    public void aFailingGetterIsUnavailableWithItsCause() {
        AttributeValue broken = rows.get("Broken");

        assertThat(broken.isAvailable()).isFalse();
        assertThat(broken.text()).isEqualTo("Unavailable");
        assertThat(broken.failure()).isEqualTo("java.lang.IllegalStateException: broken on purpose");
    }

    @Test
    public void onlyAWritableTextTypeIsEditable() {
        assertThat(rows.get("Level").isEditable()).isTrue();
        assertThat(rows.get("Name").isEditable()).isFalse();
        assertThat(rows.get("Broken").isEditable()).isFalse();
    }

    @Test
    public void anArrayOpensInTheViewer() {
        AttributeValue history = rows.get("History");

        assertThat(history.isViewable()).isTrue();
        assertThat(history.text()).isEqualTo("long[3]");
        assertThat(rows.get("Level").isViewable()).isFalse();
    }
}
