package com.druvu.jconsole.fx.tab.threads;

import com.druvu.jconsole.core.text.Messages;
import com.druvu.jconsole.core.vm.JmxDataAccess;
import com.druvu.jconsole.fx.charts.TimeRangeChooser;
import com.druvu.jconsole.fx.charts.TimeSeriesChart;
import com.druvu.jconsole.fx.charts.TitledChart;
import com.druvu.jconsole.fx.tab.TabContext;
import java.util.List;
import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;

/**
 * The Threads tab's content: the chart of live and peak threads over a chosen time range, and under it the thread list
 * with its filter, the details of the thread selected, and deadlock detection, each deadlock found in a tab of its own.
 * Built and used on the FX thread.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
final class ThreadsView {

    static final String LIVE_KEY = "threadCount";
    static final String PEAK_KEY = "peak";
    static final String FILTER_STYLE_CLASS = "thread-filter";
    static final String COUNT_STYLE_CLASS = "thread-count";
    static final String DETECT_STYLE_CLASS = "detect-deadlock";
    static final String MESSAGE_STYLE_CLASS = "deadlock-message";

    /** How long "No deadlock detected" stays on screen. */
    static final Duration MESSAGE_SHOWN_FOR = Duration.seconds(30);

    private static final double GAP = 6;
    private static final int FILTER_COLUMNS = 20;

    private final TimeSeriesChart chart = new TimeSeriesChart();
    private final TabPane lists = new TabPane();
    private final TextField filter = new TextField();
    private final Label count = new Label();
    private final Label message = new Label();
    private final PauseTransition messageTimer = new PauseTransition(MESSAGE_SHOWN_FOR);
    private final VBox root = new VBox(GAP);
    private final TabContext context;
    private final BackgroundReads reads;
    private final StackReader stacks;
    private final ThreadBrowser allThreads;

    /** The newest reading's threads, kept to filter again when the filter text changes. */
    private List<ThreadEntry> threads = List.of();

    ThreadsView(TabContext context, BackgroundReads reads) {
        this.context = context;
        this.reads = reads;
        this.stacks = new StackReader(context.connection());
        this.allThreads = new ThreadBrowser(stacks, reads);

        TitledChart chartPane = chartPane();
        Tab threadsTab = new Tab(Messages.THREADS, threadsPane());
        threadsTab.setClosable(false);
        lists.getTabs().add(threadsTab);

        root.getChildren().addAll(timeRangeBar(), chartPane, lists);
        root.setPadding(new Insets(GAP));
        VBox.setVgrow(chartPane, Priority.ALWAYS);
        VBox.setVgrow(lists, Priority.ALWAYS);
        // a collapsed chart gives its room to the thread list
        chartPane
                .expandedProperty()
                .addListener((observable, wasExpanded, expanded) ->
                        VBox.setVgrow(chartPane, expanded ? Priority.ALWAYS : Priority.NEVER));
    }

    Node root() {
        return root;
    }

    /** Puts a reading on screen: the counts on the chart, the threads in the list. */
    void show(ThreadListing listing) {
        chart.addValues(listing.timeMillis(), listing.live(), listing.peak());
        threads = listing.threads();
        showThreads();
    }

    private TitledChart chartPane() {
        chart.addSeries(LIVE_KEY, Messages.LIVE_THREADS, null, true);
        chart.addSeries(PEAK_KEY, Messages.PEAK, Color.RED, true);
        chart.setAccessibleText(Messages.THREAD_TAB_THREAD_PLOTTER_ACCESSIBLE_NAME);
        TitledChart pane = new TitledChart(Messages.NUMBER_OF_THREADS, chart);
        pane.setCollapsible(true);
        return pane;
    }

    private HBox timeRangeBar() {
        TimeRangeChooser chooser = new TimeRangeChooser(chart);
        Label label = new Label(Messages.TIME_RANGE_COLON);
        label.setLabelFor(chooser);
        HBox bar = new HBox(GAP, label, chooser);
        bar.setAlignment(Pos.CENTER);
        return bar;
    }

    /** The list of all threads, and under it the filter, the count and the deadlock search. */
    private BorderPane threadsPane() {
        filter.setPromptText("Filter");
        filter.setPrefColumnCount(FILTER_COLUMNS);
        filter.getStyleClass().add(FILTER_STYLE_CLASS);
        filter.textProperty().addListener(observable -> showThreads());
        count.getStyleClass().add(COUNT_STYLE_CLASS);
        Button detect = new Button(Messages.DETECT_DEADLOCK);
        detect.setTooltip(new Tooltip(Messages.DETECT_DEADLOCK_TOOLTIP));
        detect.getStyleClass().add(DETECT_STYLE_CLASS);
        detect.setOnAction(event -> detectDeadlock());
        message.getStyleClass().add(MESSAGE_STYLE_CLASS);
        messageTimer.setOnFinished(event -> message.setText(""));

        HBox tools = new HBox(GAP, filter, count, new Separator(Orientation.VERTICAL), detect, message);
        tools.setAlignment(Pos.CENTER_LEFT);
        tools.setPadding(new Insets(GAP, 0, 0, 0));
        BorderPane pane = new BorderPane(allThreads.root());
        pane.setBottom(tools);
        return pane;
    }

    private void showThreads() {
        List<ThreadEntry> shown = ThreadFilter.apply(threads, filter.getText());
        allThreads.showThreads(shown);
        count.setText(ThreadFilter.countText(shown.size(), threads.size()));
    }

    private void detectDeadlock() {
        message.setText("");
        JmxDataAccess connection = context.connection();
        reads.read(
                () -> DeadlockReader.read(connection),
                this::showDeadlocks,
                failure -> context.host().showError(Messages.DETECT_DEADLOCK, failure));
    }

    /** Replaces the previous search's tabs with a tab per deadlock, or says for a while that there is none. */
    private void showDeadlocks(List<DeadlockCycle> cycles) {
        lists.getTabs().remove(1, lists.getTabs().size());
        if (cycles.isEmpty()) {
            message.setText(Messages.NO_DEADLOCK_DETECTED);
            messageTimer.playFromStart();
            return;
        }
        for (int i = 0; i < cycles.size(); i++) {
            ThreadBrowser browser = new ThreadBrowser(stacks, reads);
            browser.showThreads(cycles.get(i).threads());
            browser.selectFirst();
            lists.getTabs().add(new Tab(DeadlockCycle.tabTitle(i, cycles.size()), browser.root()));
        }
        lists.getSelectionModel().select(1);
    }
}
