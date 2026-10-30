package com.druvu.jconsole.fx.tab.summary;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Stream;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/** Covers the summary page: values and paths shown, nodes kept across updates, rebuilt when rows change. */
public class SummaryViewTest {

    @BeforeClass
    public void startToolkit() {
        FxTestToolkit.ensureStarted();
    }

    @Test
    public void showsEveryValueAndPath() {
        List<String> shown = FxTestToolkit.call(() -> {
            SummaryView view = new SummaryView();
            view.show(summary("12", "a.jar\nb.jar"));
            return texts(view);
        });

        assertThat(shown).containsExactly("12", "4", "a.jar\nb.jar");
    }

    @Test
    public void anUpdateChangesTheTextsAndKeepsTheNodes() {
        boolean sameNodes = FxTestToolkit.call(() -> {
            SummaryView view = new SummaryView();
            view.show(summary("12", "a.jar"));
            List<Node> before = List.copyOf(view.page().lookupAll("." + SummaryView.VALUE_STYLE));
            view.show(summary("13", "a.jar"));
            List<Node> after = List.copyOf(view.page().lookupAll("." + SummaryView.VALUE_STYLE));
            return before.equals(after) && ((Label) after.getFirst()).getText().equals("13");
        });

        assertThat(sameNodes).isTrue();
    }

    @Test
    public void otherRowsRebuildThePage() {
        List<String> shown = FxTestToolkit.call(() -> {
            SummaryView view = new SummaryView();
            view.show(summary("12", "a.jar"));
            view.show(new VmSummary(
                    "now",
                    List.of(new SummarySection("Threads", List.of(new SummaryRow("Live threads", "7")))),
                    List.of()));
            return texts(view);
        });

        assertThat(shown).containsExactly("7");
    }

    @Test
    public void aJvmWithoutPlatformMXBeansEmptiesThePage() {
        List<String> shown = FxTestToolkit.call(() -> {
            SummaryView view = new SummaryView();
            view.show(summary("12", "a.jar"));
            view.showUnavailable();
            return texts(view);
        });

        assertThat(shown).isEmpty();
    }

    private static VmSummary summary(String liveThreads, String classPath) {
        return new VmSummary(
                "now",
                List.of(
                        new SummarySection("Threads", List.of(new SummaryRow("Live threads", liveThreads))),
                        new SummarySection("Operating system", List.of(new SummaryRow("Processors", "4")))),
                List.of(new SummaryRow("Class path", classPath)));
    }

    private static List<String> texts(SummaryView view) {
        List<String> values = view.page().lookupAll("." + SummaryView.VALUE_STYLE).stream()
                .map(node -> ((Label) node).getText())
                .toList();
        List<String> paths = view.page().lookupAll("." + SummaryView.PATH_STYLE).stream()
                .map(node -> ((TextArea) node).getText())
                .toList();
        return Stream.concat(values.stream(), paths.stream()).toList();
    }
}
