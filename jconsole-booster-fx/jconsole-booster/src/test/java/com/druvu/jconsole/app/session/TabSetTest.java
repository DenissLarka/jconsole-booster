package com.druvu.jconsole.app.session;

import static org.assertj.core.api.Assertions.assertThat;

import com.druvu.jconsole.fx.tab.ConnectionTab;
import java.util.List;
import java.util.Set;
import org.testng.annotations.Test;

/** Covers how a connection's tabs are arranged: by place, split by the switches, MBeans or the first selected. */
public class TabSetTest {

    private final FakeTab mbeans = new FakeTab("mbeans", 90);
    private final FakeTab summary = new FakeTab("summary", 50);
    private final FakeTab memory = new FakeTab("memory", 20);

    @Test
    public void theTabsAreInPlaceOrder() {
        TabSet.Arranged arranged = TabSet.arrange(List.of(mbeans, summary, memory), id -> true);

        assertThat(arranged.shown()).containsExactly(memory, summary, mbeans);
        assertThat(arranged.switchedOff()).isEmpty();
    }

    @Test
    public void aTabSwitchedOffIsSetAside() {
        Set<String> off = Set.of("summary");

        TabSet.Arranged arranged = TabSet.arrange(List.of(mbeans, summary, memory), id -> !off.contains(id));

        assertThat(arranged.shown()).containsExactly(memory, mbeans);
        assertThat(arranged.switchedOff()).containsExactly(summary);
    }

    @Test
    public void theMenuOffersEveryTabEvenTheOnesSwitchedOff() {
        assertThat(TabSet.choices(List.of(mbeans, summary)))
                .containsExactly(new TabChoice("summary", "Tab summary"), new TabChoice("mbeans", "Tab mbeans"));
    }

    @Test
    public void mbeansIsSelectedOnConnectWhenItIsThere() {
        assertThat(TabSet.selectedOnConnect(List.of(memory, summary, mbeans))).containsSame(mbeans);
    }

    @Test
    public void otherwiseTheFirstTabIsSelected() {
        List<ConnectionTab> withoutMbeans = List.of(memory, summary);

        assertThat(TabSet.selectedOnConnect(withoutMbeans)).containsSame(memory);
        assertThat(TabSet.selectedOnConnect(List.of())).isEmpty();
    }
}
