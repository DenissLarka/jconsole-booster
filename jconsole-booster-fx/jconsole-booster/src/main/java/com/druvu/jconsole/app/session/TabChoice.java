package com.druvu.jconsole.app.session;

import java.util.Objects;

/**
 * A tab the user can switch on or off in Settings &gt; Plugins.
 *
 * @param id the tab's stable name, the key of its switch
 * @param title the tab's title, shown in the menu
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
public record TabChoice(String id, String title) {

    public TabChoice {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(title, "title");
    }
}
