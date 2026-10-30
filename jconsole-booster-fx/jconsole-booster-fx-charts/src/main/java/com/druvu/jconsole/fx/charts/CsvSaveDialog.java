package com.druvu.jconsole.fx.charts;

import com.druvu.jconsole.core.text.Messages;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Locale;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.stage.FileChooser;
import javafx.stage.Window;

/**
 * The "Save data as..." flow: a file chooser for a CSV file, a confirmation before replacing a file the chooser did not
 * ask about, the export, and a notice of the result.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
final class CsvSaveDialog {

    private static final String CSV_EXTENSION = ".csv";

    private final FileChooser chooser = new FileChooser();

    CsvSaveDialog() {
        chooser.setTitle(Messages.PLOTTER_SAVE_AS_MENU_ITEM);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV file", "*" + CSV_EXTENSION));
    }

    /** Asks for a file and saves the samples of {@code store} there. */
    void show(Window owner, SampleStore store, int decimals) {
        File chosen = chooser.showSaveDialog(owner);
        if (chosen == null) {
            return;
        }
        chooser.setInitialDirectory(chosen.getParentFile());
        File target = withCsvExtension(chosen);
        // the chooser confirmed replacing the file it returned; a name we extended is not confirmed yet
        if (!target.equals(chosen) && target.exists() && !confirmReplace(owner, target)) {
            return;
        }
        save(owner, target, store, decimals);
    }

    /** {@code file}, with {@code .csv} added when its name lacks it. */
    static File withCsvExtension(File file) {
        if (file.getName().toLowerCase(Locale.ROOT).endsWith(CSV_EXTENSION)) {
            return file;
        }
        return new File(file.getPath() + CSV_EXTENSION);
    }

    private static boolean confirmReplace(Window owner, File target) {
        ButtonType replace = new ButtonType(Messages.FILE_CHOOSER_FILE_EXISTS_OK_OPTION, ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType(Messages.FILE_CHOOSER_FILE_EXISTS_CANCEL_OPTION, ButtonData.CANCEL_CLOSE);
        Alert alert = new Alert(
                AlertType.WARNING,
                PlainText.format(Messages.FILE_CHOOSER_FILE_EXISTS_MESSAGE, target.getName()),
                replace,
                cancel);
        alert.initOwner(owner);
        alert.setTitle(Messages.FILE_CHOOSER_FILE_EXISTS_TITLE);
        alert.setHeaderText(null);
        return alert.showAndWait().filter(replace::equals).isPresent();
    }

    private static void save(Window owner, File target, SampleStore store, int decimals) {
        try (Writer out = Files.newBufferedWriter(target.toPath(), StandardCharsets.UTF_8)) {
            CsvExport.write(store, decimals, out);
        } catch (IOException failure) {
            tell(
                    owner,
                    AlertType.ERROR,
                    Messages.FILE_CHOOSER_SAVE_FAILED_TITLE,
                    PlainText.format(
                            Messages.FILE_CHOOSER_SAVE_FAILED_MESSAGE,
                            target.getAbsolutePath(),
                            reason(failure, target)));
            return;
        }
        tell(
                owner,
                AlertType.INFORMATION,
                Messages.PLOTTER_SAVE_AS_MENU_ITEM,
                PlainText.format(Messages.FILE_CHOOSER_SAVED_FILE, target.getAbsolutePath(), target.length()));
    }

    /** Why a save failed: the message, or the kind of failure when the message only names the file. */
    private static String reason(IOException failure, File target) {
        String message = failure.getLocalizedMessage();
        // file-system failures such as AccessDeniedException carry just the path, which the notice shows already
        if (message == null || message.equals(target.getPath()) || message.equals(target.getAbsolutePath())) {
            return failure.getClass().getSimpleName();
        }
        return message;
    }

    private static void tell(Window owner, AlertType type, String title, String text) {
        Alert alert = new Alert(type, text);
        alert.initOwner(owner);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
