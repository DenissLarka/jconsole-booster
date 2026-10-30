package com.druvu.jconsole.fx.tab.mbeans.operations;

import com.druvu.jconsole.fx.tab.TabHost;
import com.druvu.jconsole.fx.tab.mbeans.common.TabServices;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.stage.FileChooser;
import javax.management.MBeanOperationInfo;

/**
 * Offers a file an operation returned: Open, Download or Cancel for a whitelisted type, Download or Cancel otherwise.
 * Nothing is written or launched without the user's click, and Cancel is the default, so a stray Enter does nothing.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
final class ReturnedFile {

    private static final ButtonType OPEN = new ButtonType("Open");
    private static final ButtonType DOWNLOAD = new ButtonType("Download");

    private ReturnedFile() {}

    static void offer(TabServices services, MBeanOperationInfo operation, String mime, byte[] data) {
        String fileName = MimeRules.fileName(
                operation.getName(),
                mime,
                MimeRules.suggestedName(operation.getDescription()).orElse(null));
        boolean openable = MimeRules.isOpenable(mime);
        Optional<ButtonType> choice = ask(services.host(), fileName, mime, openable);
        if (choice.isEmpty()) {
            return;
        }
        if (choice.get() == OPEN) {
            open(services, fileName, data);
        } else if (choice.get() == DOWNLOAD) {
            save(services.host(), fileName, data);
        }
    }

    private static Optional<ButtonType> ask(TabHost host, String fileName, String mime, boolean openable) {
        List<ButtonType> buttons = new ArrayList<>();
        if (openable) {
            buttons.add(OPEN);
        }
        buttons.add(DOWNLOAD);
        buttons.add(ButtonType.CANCEL);
        String advice = openable
                ? "Open it with the default application, or download it to disk."
                : "This type is not opened automatically. Download it only if you trust the server.";
        String message = "This operation returned a file:\n\n    " + fileName + "\n    type: " + mime + "\n\n" + advice;
        Alert alert = new Alert(
                openable ? Alert.AlertType.CONFIRMATION : Alert.AlertType.WARNING,
                message,
                buttons.toArray(ButtonType[]::new));
        alert.initOwner(host.window());
        alert.setTitle("Returned file");
        alert.setHeaderText(null);
        for (ButtonType button : buttons) {
            ((Button) alert.getDialogPane().lookupButton(button)).setDefaultButton(button == ButtonType.CANCEL);
        }
        return alert.showAndWait();
    }

    /** Opens the file with the OS application, off the FX thread; without one, offers to save it instead. */
    private static void open(TabServices services, String fileName, byte[] data) {
        services.load(
                "Open " + fileName,
                () -> openWithDesktop(fileName, data),
                opened -> {
                    if (!opened) {
                        save(services.host(), fileName, data);
                    }
                },
                "Could not open the returned file");
    }

    private static boolean openWithDesktop(String fileName, byte[] data) throws IOException {
        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            return false;
        }
        // a folder of its own keeps the file's name, which picks the OS application
        Path folder = Files.createTempDirectory("jcb-open-");
        Path file = folder.resolve(fileName);
        Files.write(file, data);
        // deleted at exit in reverse order: the file first, then its folder
        folder.toFile().deleteOnExit();
        file.toFile().deleteOnExit();
        Desktop.getDesktop().open(file.toFile());
        return true;
    }

    private static void save(TabHost host, String fileName, byte[] data) {
        FileChooser chooser = new FileChooser();
        chooser.setInitialFileName(fileName);
        File target = chooser.showSaveDialog(host.window());
        if (target == null) {
            return;
        }
        try {
            Files.write(target.toPath(), data);
        } catch (IOException e) {
            host.showError("Could not save " + target, e);
        }
    }
}
