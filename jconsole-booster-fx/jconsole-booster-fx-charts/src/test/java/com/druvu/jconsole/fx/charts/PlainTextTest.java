package com.druvu.jconsole.fx.charts;

import static org.assertj.core.api.Assertions.assertThat;

import com.druvu.jconsole.core.text.Messages;
import org.testng.annotations.Test;

/** Checks that the shared HTML messages read as plain text in a dialog. */
public class PlainTextTest {

    @Test
    public void lineBreaksBecomeNewLinesAndOtherTagsGo() {
        assertThat(PlainText.withoutTags(Messages.FILE_CHOOSER_SAVE_FAILED_MESSAGE))
                .isEqualTo("Save to file failed:\n{0}\n{1}");
    }

    @Test
    public void argumentsAreNeverReadAsTags() {
        assertThat(PlainText.format(Messages.FILE_CHOOSER_SAVED_FILE, "data/<b>.csv", 12))
                .isEqualTo("Saved to file:\ndata/<b>.csv\n(12 bytes)");
    }
}
