package com.druvu.jconsole.app.connect;

import static org.assertj.core.api.Assertions.assertThat;

import com.druvu.jconsole.app.FxTestToolkit;
import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Platform;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/** Covers asking on the FX thread: from a worker it waits for the answer, on the FX thread it asks in place. */
public class OnFxThreadTest {

    @BeforeClass
    public void startToolkit() {
        FxTestToolkit.ensureStarted();
    }

    @Test
    public void aWorkerGetsTheAnswerGivenOnTheFxThread() {
        AtomicReference<Boolean> askedOnFx = new AtomicReference<>();

        String answer = OnFxThread.ask(
                () -> {
                    askedOnFx.set(Platform.isFxApplicationThread());
                    return "trust";
                },
                "cancel");

        assertThat(answer).isEqualTo("trust");
        assertThat(askedOnFx.get()).isTrue();
    }

    @Test
    public void onTheFxThreadTheQuestionIsAskedInPlace() {
        String answer = FxTestToolkit.call(() -> OnFxThread.ask(() -> "trust", "cancel"));

        assertThat(answer).isEqualTo("trust");
    }

    @Test
    public void aQuestionThatFailsGetsTheSafeAnswer() {
        String answer = OnFxThread.ask(
                () -> {
                    throw new IllegalStateException("no dialog");
                },
                "cancel");

        assertThat(answer).isEqualTo("cancel");
    }
}
