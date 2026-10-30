package com.druvu.jconsole.app.window;

import static org.assertj.core.api.Assertions.assertThat;

import javafx.scene.paint.Color;
import org.testng.annotations.Test;

/** Covers the launch colour: #RRGGBB and #RRGGBBAA are read, nothing or a non-colour gives no band. */
public class ColorBandTest {

    @Test
    public void readsAColourWithAndWithoutAlpha() {
        assertThat(ColorBand.parse("#8BC6E1")).contains(Color.rgb(0x8B, 0xC6, 0xE1));
        assertThat(ColorBand.parse("#8BC6E180")).contains(Color.rgb(0x8B, 0xC6, 0xE1, 0x80 / 255.0));
    }

    @Test
    public void noColourGivesNoBand() {
        assertThat(ColorBand.parse(null)).isEmpty();
        assertThat(ColorBand.parse(" ")).isEmpty();
        assertThat(ColorBand.parse("#12")).isEmpty();
        assertThat(ColorBand.parse("#nothex")).isEmpty();
    }
}
