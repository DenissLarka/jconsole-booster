package com.druvu.jconsole.fx.tab.mbeans;

import static org.assertj.core.api.Assertions.assertThat;

import com.druvu.jconsole.fx.tab.mbeans.common.MBeanAccess;
import com.druvu.jconsole.fx.tab.mbeans.common.TabServices;
import com.druvu.jconsole.fx.tab.mbeans.operations.OperationsView;
import com.druvu.jconsole.fx.tab.mbeans.testing.TestServers;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javax.management.JMException;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import javax.management.StandardMBean;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/** An operation runs once per Invoke: the button waits for the call to end before it takes another. */
public class OperationInvokeTest {

    private static final ObjectName SLOW = TestServers.name("test:type=Slow");

    /** An operation that holds until the test releases it. */
    public interface Slow {
        void work() throws InterruptedException;
    }

    @BeforeClass
    public void startToolkit() {
        FxTestToolkit.ensureStarted();
    }

    @Test
    public void aSecondClickWhileTheCallRunsDoesNothing() throws JMException {
        AtomicInteger calls = new AtomicInteger();
        CountDownLatch release = new CountDownLatch(1);
        Slow slow = () -> {
            calls.incrementAndGet();
            if (!release.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("the test never released the call");
            }
        };
        MBeanServer server = MBeanServerFactory.newMBeanServer();
        server.registerMBean(new StandardMBean(slow, Slow.class), SLOW);
        List<MBeanOperationInfo> operations = List.of(server.getMBeanInfo(SLOW).getOperations());
        TabServices services = new TabServices(new MBeanAccess(server), new RecordingHost());
        try {
            Button work = FxTestToolkit.call(() -> invokeButton(services, operations));

            FxTestToolkit.runAndWait(() -> {
                work.fire();
                work.fire();
            });
            awaitTrue(() -> calls.get() >= 1);

            assertThat(FxTestToolkit.call(work::isDisabled))
                    .as("the button while the call runs")
                    .isTrue();

            release.countDown();
            awaitTrue(() -> !FxTestToolkit.call(work::isDisabled));
            assertThat(calls.get()).isEqualTo(1);
        } finally {
            release.countDown();
            services.close();
        }
    }

    /** The operation's button in the operations table. */
    private static Button invokeButton(TabServices services, List<MBeanOperationInfo> operations) {
        OperationsView view = new OperationsView(services, new MemoryOnlyOperationMemory(), SLOW, "Slow", operations);
        GridPane grid = (GridPane) ((ScrollPane) view.node()).getContent();
        VBox cell = (VBox) grid.getChildren().stream()
                .filter(node -> node instanceof VBox && GridPane.getColumnIndex(node) == 0)
                .findFirst()
                .orElseThrow();
        return (Button) cell.getChildren().get(1);
    }

    private static void awaitTrue(BooleanSupplier condition) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() > deadline) {
                throw new AssertionError("timed out");
            }
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AssertionError("interrupted", e);
            }
        }
    }
}
