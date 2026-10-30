package com.druvu.jconsole.protocol.jmxmp;

import com.druvu.jconsole.core.connect.Transport;
import com.druvu.lib.loader.ComponentFactory;
import com.druvu.lib.loader.Dependencies;

/**
 * Registers {@link JmxmpTransport} with druvu-lib-loader, which is how the core finds it at run time.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
public final class JmxmpTransportFactory implements ComponentFactory<Transport> {

    @Override
    public Transport createComponent(Dependencies dependencies) {
        return new JmxmpTransport();
    }

    @Override
    public Class<Transport> type() {
        return Transport.class;
    }
}
