package com.druvu.jconsole.fx.charts;

import com.druvu.jconsole.core.text.Resources;

/**
 * Turns the shared messages' simple HTML (line breaks, centring) into plain text for a JavaFX dialog.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
final class PlainText {

    private PlainText() {}

    /** Formats {@code htmlPattern} with {@code arguments}; the tags go first, so an argument is never read as one. */
    static String format(String htmlPattern, Object... arguments) {
        return Resources.format(withoutTags(htmlPattern), arguments);
    }

    static String withoutTags(String html) {
        return html.replace("<br>", "\n").replaceAll("<[^>]*>", "");
    }
}
