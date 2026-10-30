package com.druvu.jconsole.fx.tab.threads;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.testng.annotations.Test;

/** Bringing a shown list to the wanted one in place: new items where they belong, gone items out. */
public class ListMergeTest {

    @Test
    public void fillsAnEmptyList() {
        List<String> shown = new ArrayList<>();

        ListMerge.merge(shown, List.of("a", "b", "c"));

        assertThat(shown).containsExactly("a", "b", "c");
    }

    @Test
    public void insertsNewItemsWhereTheWantedOrderPutsThem() {
        List<String> shown = new ArrayList<>(List.of("b", "d"));

        ListMerge.merge(shown, List.of("a", "b", "c", "d", "e"));

        assertThat(shown).containsExactly("a", "b", "c", "d", "e");
    }

    @Test
    public void removesTheItemsNoLongerWanted() {
        List<String> shown = new ArrayList<>(List.of("a", "b", "c", "d"));

        ListMerge.merge(shown, List.of("b", "d"));

        assertThat(shown).containsExactly("b", "d");
    }

    @Test
    public void removesAndInsertsInOneGo() {
        List<String> shown = new ArrayList<>(List.of("a", "c", "e"));

        ListMerge.merge(shown, List.of("b", "c", "d"));

        assertThat(shown).containsExactly("b", "c", "d");
    }
}
