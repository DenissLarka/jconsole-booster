package com.druvu.jconsole.app.window;

import java.lang.module.ModuleDescriptor;
import java.util.function.Consumer;
import javafx.scene.control.Alert;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

/**
 * The About box: the app's version, the Java runtime it runs on, and where to read more or report a problem.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
public final class AboutBox {

    /** The README, which is the user guide. */
    public static final String USER_GUIDE = "https://github.com/DenissLarka/jconsole-booster#readme";

    /** Where users report problems and ask for features. */
    public static final String FEEDBACK = "https://github.com/DenissLarka/jconsole-booster/issues";

    private static final int ICON_SIZE = 64;

    private AboutBox() {}

    /**
     * Shows the About box and waits until it is closed.
     *
     * @param owner the window it belongs to
     * @param openLink opens an address in the browser
     */
    public static void show(Window owner, Consumer<String> openLink) {
        Alert about = new Alert(Alert.AlertType.INFORMATION);
        about.initOwner(owner);
        about.setTitle("About " + WindowTitle.APP_NAME);
        about.setHeaderText((WindowTitle.APP_NAME + " " + version()).strip());
        about.setGraphic(new ImageView(AppIcon.image(ICON_SIZE)));
        about.getDialogPane()
                .setContent(new VBox(
                        8,
                        new Label("A maintained JConsole fork for JVM operations."),
                        new Label(javaRuntime()),
                        link("User guide", USER_GUIDE, openLink),
                        link("Feedback and issues", FEEDBACK, openLink)));
        about.showAndWait();
    }

    /** The version the app module was built with; empty in a run from the class path, which has no module. */
    static String version() {
        ModuleDescriptor descriptor = AboutBox.class.getModule().getDescriptor();
        return descriptor == null ? "" : descriptor.rawVersion().orElse("");
    }

    static String javaRuntime() {
        return "Java " + System.getProperty("java.runtime.version") + " (" + System.getProperty("java.vendor") + ")";
    }

    private static Hyperlink link(String text, String address, Consumer<String> openLink) {
        Hyperlink link = new Hyperlink(text);
        link.setOnAction(event -> openLink.accept(address));
        return link;
    }
}
