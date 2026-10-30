package com.druvu.jconsole.app.session;

import com.druvu.jconsole.fx.tab.ConnectionTab;
import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.scene.Node;

/** A tab that counts its refreshes and steps; it can be told to fail its next refresh. It never builds content. */
final class FakeTab implements ConnectionTab {

    private final String id;
    private final int place;
    private final AtomicInteger refreshes = new AtomicInteger();
    private final AtomicInteger steps = new AtomicInteger();
    private final Deque<Exception> failures = new ArrayDeque<>();

    FakeTab(String id, int place) {
        this.id = id;
        this.place = place;
    }

    /**
     * The next refresh throws {@code failure}: an {@link IOException}, an {@link UndeclaredThrowableException} as an
     * MXBean proxy throws, or an {@link IllegalStateException} as a buggy tab does.
     */
    FakeTab failNextRefreshWith(Exception failure) {
        failures.add(failure);
        return this;
    }

    int refreshes() {
        return refreshes.get();
    }

    int steps() {
        return steps.get();
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String title() {
        return "Tab " + id;
    }

    @Override
    public int place() {
        return place;
    }

    @Override
    public Node content() {
        throw new UnsupportedOperationException("a fake tab has no content");
    }

    @Override
    public Runnable refresh() throws IOException {
        refreshes.incrementAndGet();
        Exception failure = failures.poll();
        if (failure instanceof IOException connectionGone) {
            throw connectionGone;
        }
        if (failure instanceof UndeclaredThrowableException fromProxy) {
            throw fromProxy;
        }
        if (failure instanceof IllegalStateException tabBroken) {
            throw tabBroken;
        }
        return steps::incrementAndGet;
    }

    @Override
    public String toString() {
        return id;
    }
}
