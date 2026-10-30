package com.druvu.jconsole.fx.tab.mbeans.tree;

import static com.druvu.jconsole.fx.tab.mbeans.testing.TestServers.name;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.testng.annotations.Test;

/** The tree levels of an ObjectName: the domain, then type and j2eeType, then the other keys as written. */
public class MBeanPathTest {

    @Test
    public void typeLeadsThenTheKeysAsWritten() {
        MBeanPath path = MBeanPath.of(name("java.lang:name=G1 Young Generation,type=GarbageCollector"));

        assertThat(path.steps())
                .extracting(MBeanPath.Step::label)
                .containsExactly("java.lang", "GarbageCollector", "G1 Young Generation");
        assertThat(path.leaf().tooltip()).isEqualTo("name=G1 Young Generation");
    }

    @Test
    public void j2eeTypeComesSecondAndTheOthersKeepTheirOrder() {
        MBeanPath path = MBeanPath.of(name("app:zone=b,j2eeType=Servlet,alpha=a,type=Web"));

        assertThat(path.steps())
                .extracting(MBeanPath.Step::tooltip)
                .containsExactly("app", "type=Web", "j2eeType=Servlet", "zone=b", "alpha=a");
    }

    @Test
    public void idsAreThePrefixesOfTheOrderedName() {
        MBeanPath path = MBeanPath.of(name("d:name=x,type=T"));

        assertThat(path.steps()).extracting(MBeanPath.Step::id).containsExactly("d", "d:type=T", "d:type=T,name=x");
    }

    @Test
    public void aQuotedValueStaysWhole() {
        MBeanPath path = MBeanPath.of(name("d:type=T,name=\"a,b=c\""));

        assertThat(path.steps()).extracting(MBeanPath.Step::label).containsExactly("d", "T", "\"a,b=c\"");
    }

    @Test
    public void orderedKeysMatchTheNamesKeys() {
        assertThat(List.copyOf(MBeanPath.orderedKeys(name("d:b=2,a=1,type=T")).keySet()))
                .containsExactly("type", "b", "a");
    }
}
