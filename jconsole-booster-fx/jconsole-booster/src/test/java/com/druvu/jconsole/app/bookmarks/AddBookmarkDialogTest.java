package com.druvu.jconsole.app.bookmarks;

import static org.assertj.core.api.Assertions.assertThat;

import com.druvu.jconsole.app.FxTestToolkit;
import com.druvu.jconsole.core.bookmarks.BookmarkWriter;
import java.util.List;
import javafx.scene.control.ButtonType;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/** Covers the Add bookmark dialog: the default group, OK waits for a label, a typed new group is taken. */
public class AddBookmarkDialogTest {

    @BeforeClass
    public void startToolkit() {
        FxTestToolkit.ensureStarted();
    }

    @Test
    public void startsInTheDefaultGroupAndWaitsForALabel() {
        List<Object> state = FxTestToolkit.call(() -> {
            AddBookmarkDialog dialog = new AddBookmarkDialog(null, "h:1", List.of("Prod"));
            boolean completeWithoutLabel = dialog.isComplete();
            dialog.labelField().setText("   ");
            boolean completeWithBlankLabel = dialog.isComplete();
            return List.of(
                    dialog.groupField().getEditor().getText(),
                    List.copyOf(dialog.groupField().getItems()),
                    completeWithoutLabel,
                    completeWithBlankLabel);
        });

        assertThat(state).containsExactly(BookmarkWriter.DEFAULT_GROUP, List.of("Prod"), false, false);
    }

    @Test
    public void okGivesTheLabelAndTheTypedGroup() {
        NewBookmark bookmark = FxTestToolkit.call(() -> {
            AddBookmarkDialog dialog = new AddBookmarkDialog(null, "h:1", List.of("Prod"));
            dialog.labelField().setText(" *prod-1* ");
            dialog.groupField().getEditor().setText(" New group ");
            return dialog.dialog().getResultConverter().call(ButtonType.OK);
        });

        assertThat(bookmark).isEqualTo(new NewBookmark("New group", "*prod-1*"));
    }

    @Test
    public void cancelGivesNothing() {
        NewBookmark bookmark = FxTestToolkit.call(() -> new AddBookmarkDialog(null, "h:1", List.of())
                .dialog()
                .getResultConverter()
                .call(ButtonType.CANCEL));

        assertThat(bookmark).isNull();
    }
}
