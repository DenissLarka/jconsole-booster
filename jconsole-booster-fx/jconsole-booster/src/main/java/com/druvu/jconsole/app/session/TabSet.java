package com.druvu.jconsole.app.session;

import com.druvu.jconsole.fx.tab.ConnectionTab;
import com.druvu.jconsole.fx.tab.TabContext;
import com.druvu.lib.loader.Dependencies;
import com.druvu.lib.loader.MultiComponentLoader;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * The tabs of one connection: found at run time through druvu-lib-loader, kept or dropped by the user's switches,
 * ordered by their place, and disposed through the factory that made them.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
final class TabSet {

    /** The tab shown first on connect, when it is there. */
    static final String SELECTED_ON_CONNECT = "mbeans";

    private static final Comparator<ConnectionTab> BY_PLACE =
            Comparator.comparingInt(ConnectionTab::place).thenComparing(ConnectionTab::title);

    private TabSet() {}

    /**
     * The loaded tabs split by the user's switches.
     *
     * @param shown the tabs switched on, in place order
     * @param switchedOff the tabs switched off, to dispose at once
     */
    record Arranged(List<ConnectionTab> shown, List<ConnectionTab> switchedOff) {

        Arranged {
            shown = List.copyOf(shown);
            switchedOff = List.copyOf(switchedOff);
        }
    }

    /** Creates every tab the loader finds, for one connection. */
    static List<ConnectionTab> load(TabContext context) {
        return MultiComponentLoader.loadAll(ConnectionTab.class, Dependencies.of(TabContext.class, context));
    }

    /** Disposes tabs through the factories that created them. */
    static void dispose(List<ConnectionTab> tabs) {
        if (!tabs.isEmpty()) {
            MultiComponentLoader.disposeAll(ConnectionTab.class, tabs);
        }
    }

    /** Splits the loaded tabs into those switched on, in place order, and those switched off. */
    static Arranged arrange(List<ConnectionTab> loaded, Predicate<String> isOn) {
        List<ConnectionTab> ordered = loaded.stream().sorted(BY_PLACE).toList();
        return new Arranged(
                ordered.stream().filter(tab -> isOn.test(tab.id())).toList(),
                ordered.stream().filter(tab -> !isOn.test(tab.id())).toList());
    }

    /** What View &gt; Tabs offers: every loaded tab, in place order. */
    static List<TabChoice> choices(List<ConnectionTab> loaded) {
        return loaded.stream()
                .sorted(BY_PLACE)
                .map(tab -> new TabChoice(tab.id(), tab.title()))
                .toList();
    }

    /** The tab to show on connect: MBeans when it is there, else the first. */
    static Optional<ConnectionTab> selectedOnConnect(List<ConnectionTab> shown) {
        return shown.stream()
                .filter(tab -> SELECTED_ON_CONNECT.equals(tab.id()))
                .findFirst()
                .or(() -> shown.stream().findFirst());
    }
}
