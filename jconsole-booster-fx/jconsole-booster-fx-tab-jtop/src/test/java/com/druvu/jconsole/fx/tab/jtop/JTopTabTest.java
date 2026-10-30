package com.druvu.jconsole.fx.tab.jtop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/** The JTop tab against this JVM: its place among the tabs, a refresh that fills its content, a lost connection. */
public class JTopTabTest {

    @BeforeClass
    public void startFx() {
        FxTestToolkit.ensureStarted();
    }

    @Test
    public void namesItselfJTopAtPlaceSixty() {
        JTopTab tab = new JTopTab(ManagementFactory::getThreadMXBean);

        assertThat(tab.id()).isEqualTo("jtop");
        assertThat(tab.title()).isEqualTo("JTop");
        assertThat(tab.place()).isEqualTo(60);
    }

    @Test
    public void aRefreshFillsTheContent() throws IOException {
        JTopTab tab = new JTopTab(ManagementFactory::getThreadMXBean);
        Node content = FxTestToolkit.call(tab::content);

        Runnable show = tab.refresh();
        FxTestToolkit.runAndWait(show);

        int shownRows = FxTestToolkit.call(
                () -> ((TableView<?>) content.lookup(".table-view")).getItems().size());
        String status = FxTestToolkit.call(() -> ((Label) content.lookup("." + JTopView.STATUS_STYLE_CLASS)).getText());
        assertThat(shownRows).isPositive();
        assertThat(status).isEqualTo("Threads: " + shownRows);
        assertThat(FxTestToolkit.call(tab::content)).isSameAs(content);
    }

    @Test
    public void aLostConnectionIsAnIoException() {
        ThreadMXBean lost = new ThreadBeanStub()
                .answer("isThreadCpuTimeSupported", args -> {
                    throw new IOException("connection lost");
                })
                .bean();
        JTopTab tab = new JTopTab(() -> lost);

        assertThatThrownBy(tab::refresh).isInstanceOf(IOException.class).hasMessage("connection lost");
    }
}
