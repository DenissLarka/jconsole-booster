package com.druvu.jconsole.app.connect;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;
import javafx.application.Platform;
import lombok.extern.slf4j.Slf4j;

/**
 * Asks a question on the FX thread and waits for the answer. The certificate prompt is called on the connecting worker,
 * and a dialog can only be shown on the FX thread.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
@Slf4j
final class OnFxThread {

    private OnFxThread() {}

    /**
     * The answer to {@code question}, asked on the FX thread.
     *
     * @param question shows a dialog and returns the user's answer
     * @param failClosed the answer when the question cannot be asked or fails: the safe choice
     */
    static <T> T ask(Supplier<T> question, T failClosed) {
        if (Platform.isFxApplicationThread()) {
            // Waiting here would block the thread that has to show the dialog; a dialog's own event loop runs it.
            return askHere(question, failClosed);
        }
        CompletableFuture<T> answer = new CompletableFuture<>();
        try {
            Platform.runLater(() -> answer.complete(askHere(question, failClosed)));
        } catch (IllegalStateException toolkitGone) {
            log.warn("The question cannot be asked, the JavaFX toolkit is not running");
            return failClosed;
        }
        try {
            return answer.get();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return failClosed;
        } catch (ExecutionException impossible) {
            // askHere never completes exceptionally
            return failClosed;
        }
    }

    private static <T> T askHere(Supplier<T> question, T failClosed) {
        try {
            return question.get();
        } catch (RuntimeException failure) {
            log.error("Asking the user failed, taking the safe answer", failure);
            return failClosed;
        }
    }
}
