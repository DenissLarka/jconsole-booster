package com.druvu.jconsole.app.session;

import com.druvu.jconsole.app.connect.ConnectDialog;
import com.druvu.jconsole.app.connect.ConnectErrors;
import com.druvu.jconsole.app.connect.ConnectionRequest;
import com.druvu.jconsole.app.window.MainWindow;
import com.druvu.jconsole.core.bookmarks.RecentConnections;
import com.druvu.jconsole.core.vm.ProxyClient;
import com.druvu.jconsole.fx.tab.ConnectionTab;
import com.druvu.jconsole.fx.tab.TabContext;
import com.druvu.lib.fx.exec.FxExec;
import com.druvu.lib.fx.util.FxThreads;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.extern.slf4j.Slf4j;

/**
 * The window's one connection and its life. Connecting runs off the FX thread; once connected, the tabs are created and
 * the update tick refreshes them; a lost connection is offered for reconnecting; a failed connect goes back to the
 * Connect dialog with the reason. Connecting while connected asks first, then replaces the connection. Disconnecting
 * stops the tick, disposes the tabs and closes the connection; a reconnect creates the tabs anew.
 *
 * <p>FX thread only.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
@Slf4j
public final class ConnectionController {

    private enum Phase {
        NOT_CONNECTED,
        CONNECTING,
        CONNECTED,
        LOST
    }

    private final MainWindow window;
    private final ConnectDialog connectDialog;
    private final TabSwitches switches;
    private final Duration interval;
    private final FxExec exec;
    private final ClientWork clientWork;

    private final ReadOnlyBooleanWrapper active = new ReadOnlyBooleanWrapper(this, "active", false);
    private final ReadOnlyBooleanWrapper reconnectable = new ReadOnlyBooleanWrapper(this, "reconnectable", false);
    private final ObservableList<TabChoice> tabChoices = FXCollections.observableArrayList();

    private Phase phase = Phase.NOT_CONNECTED;
    // the current or the last target: what Reconnect connects to
    private ConnectionRequest request;
    // the open (or opening, or lost) connection; null when there is none
    private ProxyClient client;
    private List<ConnectionTab> tabs = List.of();
    private UpdateTick tick;
    // each connect attempt gets a number; an attempt that is no longer the latest is ignored when it ends
    private long attempt;

    /**
     * @param window the window the connection is shown in
     * @param connectDialog asks the user what to connect to
     * @param switches the user's tab switches
     * @param interval the time between two update ticks
     * @param exec runs the blocking work off the FX thread
     */
    public ConnectionController(
            MainWindow window, ConnectDialog connectDialog, TabSwitches switches, Duration interval, FxExec exec) {
        this.window = Objects.requireNonNull(window, "window");
        this.connectDialog = Objects.requireNonNull(connectDialog, "connectDialog");
        this.switches = Objects.requireNonNull(switches, "switches");
        this.interval = Objects.requireNonNull(interval, "interval");
        this.exec = Objects.requireNonNull(exec, "exec");
        this.clientWork = new ClientWork(exec);
    }

    /** True while there is a connection: open, opening or lost. Disconnect follows it. */
    public ReadOnlyBooleanProperty activeProperty() {
        return active.getReadOnlyProperty();
    }

    /** True once there is a target to reconnect to. */
    public ReadOnlyBooleanProperty reconnectableProperty() {
        return reconnectable.getReadOnlyProperty();
    }

    /** The tabs of the last connection, for View &gt; Tabs; empty before the first connect. */
    public ObservableList<TabChoice> tabChoices() {
        return FXCollections.unmodifiableObservableList(tabChoices);
    }

    /** The current target in {@code host:port} form, for a bookmark; empty without a connection. */
    public String currentTarget() {
        return phase == Phase.NOT_CONNECTED || request == null ? "" : request.shortUrl();
    }

    /** Opens the Connect dialog with the current target, then connects to what the user chose. */
    public void newConnection() {
        newConnection(request == null ? "" : request.shortUrl());
    }

    /** Opens the Connect dialog with {@code target} filled in, then connects to what the user chose. */
    public void newConnection(String target) {
        connectDialog.ask(ConnectionRequest.canonical(target), null, null).ifPresent(this::connect);
    }

    /** Connects to {@code next}; an open connection is replaced, once the user agrees. */
    public void connect(ConnectionRequest next) {
        if (phase != Phase.NOT_CONNECTED
                && !window.confirm(
                        "Replace the connection",
                        "Disconnect from " + connectionName() + " and connect to " + next.shortUrl() + "?",
                        "Replace")) {
            return;
        }
        close();
        open(next);
    }

    /** Closes the connection and opens it again; the tabs are created anew. */
    public void reconnect() {
        if (request != null) {
            ConnectionRequest same = request;
            close();
            open(same);
        }
    }

    /** Closes the connection; Reconnect can open it again. */
    public void disconnect() {
        close();
    }

    /** At exit: stops everything and closes the connection on this thread, as no worker is left to wait for. */
    public void shutdown() {
        attempt++;
        stopTick();
        TabSet.dispose(tabs);
        tabs = List.of();
        if (client != null) {
            client.disconnect();
            client = null;
        }
    }

    private void open(ConnectionRequest next) {
        ProxyClient nextClient;
        try {
            nextClient =
                    ProxyClient.getProxyClient(next.url(), next.userName(), next.password(), next.requireValidChain());
        } catch (IOException malformed) {
            askAgain(next, ConnectErrors.describe(malformed));
            return;
        }
        request = next;
        client = nextClient;
        long thisAttempt = ++attempt;
        setPhase(Phase.CONNECTING);
        clientWork
                .run(nextClient, "Connecting to " + next.shortUrl(), nextClient::connect)
                .whenCompleteAsync((ignored, failure) -> connectEnded(thisAttempt, nextClient), FxThreads.fxExecutor());
    }

    private void connectEnded(long endedAttempt, ProxyClient attempted) {
        if (endedAttempt != attempt) {
            // the user moved on meanwhile; closing that connection is already queued behind this connect
            return;
        }
        if (!attempted.isConnected()) {
            ConnectionRequest failed = request;
            client = null;
            setPhase(Phase.NOT_CONNECTED);
            log.debug("Connecting to {} failed", failed.shortUrl(), attempted.getLastConnectException());
            askAgain(failed, ConnectErrors.describe(attempted.getLastConnectException()));
            return;
        }
        String target = request.shortUrl();
        exec.run("Remembering " + target, () -> RecentConnections.record(target));
        setPhase(Phase.CONNECTED);
        openTabs(attempted);
    }

    /** The Connect dialog again, filled with the failed target and why it failed. */
    private void askAgain(ConnectionRequest failed, String reason) {
        connectDialog.ask(failed.shortUrl(), failed.userName(), reason).ifPresent(this::connect);
    }

    private void openTabs(ProxyClient connected) {
        try {
            List<ConnectionTab> loaded = TabSet.load(new TabContext(connected, window));
            tabChoices.setAll(TabSet.choices(loaded));
            TabSet.Arranged arranged = TabSet.arrange(loaded, switches::isOn);
            TabSet.dispose(arranged.switchedOff());
            tabs = arranged.shown();
            window.showTabs(tabs, TabSet.selectedOnConnect(tabs).orElse(null));
        } catch (RuntimeException failure) {
            log.error("Creating the tabs failed", failure);
            window.showError("The tabs could not be created", failure);
        }
        tick = new UpdateTick(connected::flush, tabs, FxThreads.fxExecutor(), new TickListener());
        tick.start(interval);
    }

    private void onConnectionLost(Exception cause) {
        log.warn("Lost the connection to {}", connectionName(), cause);
        stopTick();
        ProxyClient lost = client;
        clientWork.run(lost, "Closing the lost connection", lost::markAsDead);
        setPhase(Phase.LOST);
        if (window.confirm(
                "Connection lost",
                "The connection to " + connectionName() + " was lost.\n" + cause.getMessage(),
                "Reconnect")) {
            reconnect();
        }
    }

    /** Stops the tick, disposes the tabs and closes the connection off the FX thread. */
    private void close() {
        attempt++;
        stopTick();
        if (!tabs.isEmpty()) {
            window.clearTabs();
            TabSet.dispose(tabs);
            tabs = List.of();
        }
        if (client != null) {
            ProxyClient closing = client;
            client = null;
            clientWork.run(closing, "Disconnecting from " + closing.getDisplayName(), closing::disconnect);
        }
        setPhase(Phase.NOT_CONNECTED);
    }

    private void stopTick() {
        if (tick != null) {
            tick.stop();
            tick = null;
        }
    }

    private void setPhase(Phase next) {
        phase = next;
        active.set(next != Phase.NOT_CONNECTED);
        reconnectable.set(request != null);
        String name = connectionName();
        switch (next) {
            case NOT_CONNECTED -> window.showNotConnected("Not connected");
            case CONNECTING -> window.showNotConnected("Connecting to " + name);
            case CONNECTED -> window.showConnected(name, "Connected to " + name);
            case LOST -> window.showNotConnected("Connection to " + name + " lost");
        }
    }

    /** The connection's name as the core gives it: {@code user@url}, or the URL without a user. */
    private String connectionName() {
        return request == null ? "" : ProxyClient.getConnectionName(request.url(), request.userName());
    }

    /** Hears the current tick; a stopped tick says nothing more, so a stale report never arrives. */
    private final class TickListener implements UpdateTick.Listener {

        @Override
        public void tabFailed(ConnectionTab tab, RuntimeException failure) {
            window.disableTab(tab);
            window.showError("The " + tab.title() + " tab stopped updating", failure);
        }

        @Override
        public void connectionLost(Exception cause) {
            onConnectionLost(cause);
        }

        @Override
        public void updated(LocalTime at) {
            window.showLastUpdate(at);
        }
    }
}
