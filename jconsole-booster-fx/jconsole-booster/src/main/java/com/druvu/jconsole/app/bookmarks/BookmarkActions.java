package com.druvu.jconsole.app.bookmarks;

import com.druvu.jconsole.app.window.MainWindow;
import com.druvu.jconsole.core.bookmarks.BookmarkWriter;
import com.druvu.lib.fx.exec.FxExec;
import com.druvu.lib.fx.util.FxThreads;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import javafx.stage.Window;
import lombok.extern.slf4j.Slf4j;

/**
 * What the user does with bookmarks besides picking one: add the connection as a bookmark, and open the file in an
 * editor. The file work runs off the FX thread; the outcome comes back as a notice or an error. FX thread only.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
@Slf4j
public final class BookmarkActions {

    private final BookmarksFile file;
    private final MainWindow window;
    private final FxExec exec;

    /**
     * @param file the bookmarks file
     * @param window where notices and errors go
     * @param exec runs the file work off the FX thread
     */
    public BookmarkActions(BookmarksFile file, MainWindow window, FxExec exec) {
        this.file = Objects.requireNonNull(file, "file");
        this.window = Objects.requireNonNull(window, "window");
        this.exec = Objects.requireNonNull(exec, "exec");
    }

    /**
     * Asks for a label and group, then adds {@code target} to the bookmarks file.
     *
     * @param owner the window to put the dialog over
     * @param target the connection's target, {@code host:port} or a service URL
     */
    public void add(Window owner, String target) {
        Optional<NewBookmark> bookmark =
                new AddBookmarkDialog(owner, target, file.groupNames()).dialog().showAndWait();
        if (bookmark.isEmpty()) {
            return;
        }
        NewBookmark added = bookmark.get();
        // BookmarkWriter writes to the user's home file, the one this app reads (BoosterHome.connectionsFile()).
        exec.run("Adding a bookmark", () -> BookmarkWriter.appendBookmark(added.group(), added.label(), target))
                .whenCompleteAsync(
                        (ignored, failure) -> {
                            if (failure == null) {
                                window.notice("Bookmarked " + target + " under " + added.group());
                            } else {
                                window.showError("Could not add the bookmark", failure);
                            }
                        },
                        FxThreads.fxExecutor());
    }

    /** Opens the bookmarks file in the system's editor, or with the default app when there is no editor. */
    public void openFile() {
        exec.run(
                        "Opening the bookmarks file",
                        () -> openInEditor(file.ensureExists().toFile()))
                .whenCompleteAsync(
                        (ignored, failure) -> {
                            if (failure != null) {
                                window.showError("Could not open the bookmarks file", failure);
                            }
                        },
                        FxThreads.fxExecutor());
    }

    private static void openInEditor(File bookmarks) throws IOException {
        if (!Desktop.isDesktopSupported()) {
            throw new IOException("This system cannot open files from Java; the file is " + bookmarks);
        }
        Desktop desktop = Desktop.getDesktop();
        if (desktop.isSupported(Desktop.Action.EDIT)) {
            try {
                desktop.edit(bookmarks);
                return;
            } catch (IOException noEditor) {
                // some systems offer EDIT and then have no editor for .txt: open it instead
                log.debug("No editor for {}, opening it instead", bookmarks, noEditor);
            }
        }
        if (!desktop.isSupported(Desktop.Action.OPEN)) {
            throw new IOException("This system can neither edit nor open " + bookmarks);
        }
        desktop.open(bookmarks);
    }
}
