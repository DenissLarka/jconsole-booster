package com.druvu.jconsole.ui.core;

import com.druvu.jconsole.core.vm.ConnectionStateListener;
import com.druvu.jconsole.core.vm.ProxyClient;
import com.sun.tools.jconsole.JConsoleContext;
import java.beans.PropertyChangeListener;
import javax.management.MBeanServerConnection;
import javax.swing.event.SwingPropertyChangeSupport;

/**
 * The connection as JConsole's Swing plugin API sees it: a {@link JConsoleContext} over the core's {@link ProxyClient}.
 * Each change of the connection state is fired again as a {@code connectionState} property change on the event dispatch
 * thread, where JConsole's plugins and panels expect it.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
public final class SwingConnectionContext implements JConsoleContext {

    private final ProxyClient proxyClient;
    private final SwingPropertyChangeSupport propertyChangeSupport = new SwingPropertyChangeSupport(this, true);
    private final ConnectionStateListener stateListener = this::fireStateChange;

    public SwingConnectionContext(ProxyClient proxyClient) {
        this.proxyClient = proxyClient;
        proxyClient.addConnectionStateListener(stateListener);
    }

    @Override
    public MBeanServerConnection getMBeanServerConnection() {
        return proxyClient.getMBeanServerConnection();
    }

    @Override
    public ConnectionState getConnectionState() {
        return toPluginState(proxyClient.getConnectionState());
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }

    /** Stops following the connection; the panel that owns this context calls it when it closes. */
    public void close() {
        proxyClient.removeConnectionStateListener(stateListener);
    }

    private void fireStateChange(
            com.druvu.jconsole.core.vm.ConnectionState oldState, com.druvu.jconsole.core.vm.ConnectionState newState) {
        propertyChangeSupport.firePropertyChange(
                CONNECTION_STATE_PROPERTY, toPluginState(oldState), toPluginState(newState));
    }

    // Inside this class the plain name ConnectionState is the plugin API's (inherited), hence the core's in full.
    private static ConnectionState toPluginState(com.druvu.jconsole.core.vm.ConnectionState state) {
        return ConnectionState.valueOf(state.name());
    }
}
