package com.druvu.jconsole.app.session;

import com.druvu.jconsole.core.vm.ProxyClient;
import com.druvu.lib.fx.exec.FxExec;
import com.druvu.lib.fx.exec.ThrowingRunnable;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Runs the blocking work on a connection (connect, disconnect) off the FX thread, one piece at a time per connection.
 * The core hands out one client per target, so a reconnect to the same target reuses the client; without the order a
 * disconnect could land after the connect that follows it. Work on different clients runs side by side, so a slow
 * target never holds up another.
 *
 * <p>FX thread only.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
final class ClientWork {

    private final FxExec exec;
    private final Map<ProxyClient, CompletableFuture<Void>> lastWork = new IdentityHashMap<>();

    ClientWork(FxExec exec) {
        this.exec = Objects.requireNonNull(exec, "exec");
    }

    /**
     * Runs {@code work} once the client's earlier work has ended, however it ended.
     *
     * @param title what the status bar shows while it runs
     * @return completes when this work has ended
     */
    CompletableFuture<Void> run(ProxyClient client, String title, ThrowingRunnable work) {
        CompletableFuture<Void> previous = lastWork.getOrDefault(client, CompletableFuture.completedFuture(null));
        CompletableFuture<Void> next =
                previous.handle((ignored, failure) -> null).thenCompose(ignored -> exec.run(title, work));
        lastWork.put(client, next);
        return next;
    }
}
