package com.druvu.jconsole.app.bookmarks;

import java.util.Objects;

/**
 * A bookmark the user is adding: where it goes and what it is called. The target comes from the connection.
 *
 * @param group the group it goes under, existing or new
 * @param label its label, display markup allowed
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
record NewBookmark(String group, String label) {

    NewBookmark {
        Objects.requireNonNull(group, "group");
        Objects.requireNonNull(label, "label");
    }
}
