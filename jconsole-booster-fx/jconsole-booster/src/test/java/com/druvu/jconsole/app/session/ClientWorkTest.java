package com.druvu.jconsole.app.session;

import static org.assertj.core.api.Assertions.assertThat;

import com.druvu.jconsole.core.vm.ProxyClient;
import com.druvu.lib.fx.exec.FxExec;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

/** Covers the order of connection work: one at a time per client, after a failure too, side by side across clients. */
public class ClientWorkTest {

    private static final long TIMEOUT_SECONDS = 10;

    private final FxExec exec = new FxExec();

    @AfterClass(alwaysRun = true)
    public void closeExec() {
        exec.close();
    }

    @Test
    public void workOnOneClientRunsInOrder() throws IOException {
        ClientWork work = new ClientWork(exec);
        ProxyClient client = client(1);
        CountDownLatch release = new CountDownLatch(1);
        List<String> done = new CopyOnWriteArrayList<>();

        work.run(client, "connect", () -> {
            awaitRelease(release);
            done.add("connect");
        });
        CompletableFuture<Void> disconnect = work.run(client, "disconnect", () -> done.add("disconnect"));
        release.countDown();
        disconnect.orTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS).join();

        assertThat(done).containsExactly("connect", "disconnect");
    }

    @Test
    public void aFailureDoesNotStopTheNextWork() throws IOException {
        ClientWork work = new ClientWork(exec);
        ProxyClient client = client(2);
        List<String> done = new CopyOnWriteArrayList<>();

        work.run(client, "connect", () -> {
            throw new IOException("refused");
        });
        work.run(client, "disconnect", () -> done.add("disconnect"))
                .orTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .join();

        assertThat(done).containsExactly("disconnect");
    }

    @Test
    public void anotherClientDoesNotWait() throws IOException {
        ClientWork work = new ClientWork(exec);
        CountDownLatch release = new CountDownLatch(1);

        CompletableFuture<Void> slow = work.run(client(3), "slow", () -> awaitRelease(release));
        work.run(client(4), "fast", () -> {})
                .orTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .join();

        assertThat(slow).isNotDone();
        release.countDown();
        slow.orTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS).join();
    }

    private static void awaitRelease(CountDownLatch release) throws InterruptedException {
        assertThat(release.await(TIMEOUT_SECONDS, TimeUnit.SECONDS))
                .as("released in time")
                .isTrue();
    }

    /** A client that is never connected: creating one opens nothing. */
    private static ProxyClient client(int port) throws IOException {
        return ProxyClient.getProxyClient("service:jmx:jmxmp://localhost:" + port, "work-test", null, false);
    }
}
