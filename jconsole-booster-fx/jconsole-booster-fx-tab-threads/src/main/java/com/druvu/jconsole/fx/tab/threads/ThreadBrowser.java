package com.druvu.jconsole.fx.tab.threads;

import com.druvu.jconsole.core.text.Messages;
import java.util.List;
import javafx.scene.Node;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;

/**
 * A list of threads beside the details of the one selected, read from the target JVM whenever the selection changes.
 * The Threads tab shows one for all the threads and one per deadlock. Built and used on the FX thread.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
final class ThreadBrowser {

    /** The style class of the details beside the list. */
    static final String DETAILS_STYLE_CLASS = "thread-details";

    /** Starts the details shown when a thread's details could not be read. */
    static final String READ_FAILED = "Could not read the thread: ";

    /** The list's share of the width at first. */
    private static final double LIST_SHARE = 0.3;

    private final ListView<ThreadEntry> list = new ListView<>();
    private final TextArea details = new TextArea(Messages.THREAD_TAB_INITIAL_STACK_TRACE_MESSAGE);
    private final SplitPane root = new SplitPane(list, details);
    private final StackReader stacks;
    private final BackgroundReads reads;

    ThreadBrowser(StackReader stacks, BackgroundReads reads) {
        this.stacks = stacks;
        this.reads = reads;
        details.setEditable(false);
        details.getStyleClass().add(DETAILS_STYLE_CLASS);
        details.setAccessibleText(Messages.THREAD_TAB_THREAD_INFO_ACCESSIBLE_NAME);
        root.setDividerPositions(LIST_SHARE);
        // a wider window widens the details, where the long stack lines are
        SplitPane.setResizableWithParent(list, false);
        list.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, previous, selected) -> readDetails(selected));
    }

    Node root() {
        return root;
    }

    /** Shows {@code threads}; the selection stays while the selected thread is among them. */
    void showThreads(List<ThreadEntry> threads) {
        ListMerge.merge(list.getItems(), threads);
    }

    void selectFirst() {
        list.getSelectionModel().selectFirst();
    }

    private void readDetails(ThreadEntry selected) {
        // a selected thread that ends leaves the list; its last details stay until another thread is chosen
        if (selected == null) {
            return;
        }
        reads.read(
                () -> stacks.read(selected.id()),
                text -> showDetails(selected, text),
                failure -> showDetails(selected, READ_FAILED + failure.getMessage()));
    }

    /** Shows the details read for {@code thread}, unless the user has chosen another thread meanwhile. */
    private void showDetails(ThreadEntry thread, String text) {
        if (thread.equals(list.getSelectionModel().getSelectedItem())) {
            details.setText(text);
            details.setScrollTop(0);
        }
    }
}
