package com.druvu.jconsole.fx.tab;

import java.io.IOException;
import javafx.scene.Node;

/**
 * One tab of the connection window. The app creates every tab at connect through druvu-lib-loader (a
 * {@code ComponentFactory<ConnectionTab>} given a {@link TabContext}), shows them sorted by {@link #place()}, refreshes
 * them on every update tick, and disposes them at disconnect through their factory. A reconnect creates them anew.
 *
 * <p>Threads: {@link #content()} is called on the FX thread; {@link #refresh()} on the update thread, never on the FX
 * thread, because JMX calls block; the step it returns runs on the FX thread.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
public interface ConnectionTab {

    /** A stable name for the tab, the key of its switch in the app's on/off list (e.g. {@code memory}). */
    String id();

    /** The tab's title. */
    String title();

    /** Where the tab sits among the others: a lower place comes first. */
    int place();

    /** The tab's content, built once. */
    Node content();

    /**
     * Reads the values the tab shows from the connection.
     *
     * @return the step that puts the values on screen, run on the FX thread
     * @throws IOException the connection is gone; the app handles the loss
     */
    Runnable refresh() throws IOException;
}
