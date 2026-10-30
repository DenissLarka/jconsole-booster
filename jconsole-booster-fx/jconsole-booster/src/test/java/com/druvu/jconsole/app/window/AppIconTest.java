package com.druvu.jconsole.app.window;

import static org.assertj.core.api.Assertions.assertThat;

import com.druvu.jconsole.app.FxTestToolkit;
import java.util.List;
import javafx.scene.image.Image;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/** Covers the window icon: it loads from the app's resources, square, at every size from 16 to 256 pixels. */
public class AppIconTest {

    @BeforeClass
    public void startToolkit() {
        FxTestToolkit.ensureStarted();
    }

    @Test
    public void everySizeLoadsSquare() {
        List<Image> icons = AppIcon.images();

        assertThat(icons).hasSize(9).allSatisfy(icon -> {
            assertThat(icon.isError()).as("load error").isFalse();
            assertThat(icon.getWidth()).isEqualTo(icon.getHeight());
        });
        assertThat(icons).extracting(Image::getWidth).contains(16.0, 32.0, 256.0);
    }
}
