package com.druvu.jconsole.fx.tab.mbeans.operations;

import com.druvu.jconsole.fx.tab.TabHost;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;

/**
 * The input of a {@code {{file:*.csv,…}}} parameter: a chosen file whose content is the value, as UTF-8 text for a
 * {@code String} parameter and as bytes otherwise.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
final class FileInput implements ParameterInput {

    private final List<String> patterns;
    private final boolean wantsText;
    private final TabHost host;
    private final Label chosen = new Label("(none)");
    private final HBox box;
    private byte[] content;

    /**
     * @param patterns the file patterns offered in the chooser, such as {@code *.csv}; empty for any file
     * @param type the parameter's type
     */
    FileInput(List<String> patterns, String type, TabHost host) {
        this.patterns = List.copyOf(patterns);
        this.wantsText = String.class.getName().equals(type);
        this.host = host;
        Button choose = new Button("Choose file…");
        choose.setOnAction(event -> choose());
        box = new HBox(6, choose, chosen);
        box.setAlignment(Pos.CENTER_LEFT);
    }

    @Override
    public Node node() {
        return box;
    }

    @Override
    public Object value() {
        if (content == null) {
            return wantsText ? "" : new byte[0];
        }
        return wantsText ? new String(content, StandardCharsets.UTF_8) : content.clone();
    }

    @Override
    public boolean isRemembered() {
        return false;
    }

    @Override
    public void onForget(Runnable forget) {
        // nothing is remembered for a file
    }

    private void choose() {
        FileChooser chooser = new FileChooser();
        if (!patterns.isEmpty()) {
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(String.join(", ", patterns), patterns));
        }
        File file = chooser.showOpenDialog(
                box.getScene() == null ? null : box.getScene().getWindow());
        if (file == null) {
            return;
        }
        try {
            content = Files.readAllBytes(file.toPath());
            chosen.setText(file.getName() + " (" + content.length + " bytes)");
        } catch (IOException e) {
            host.showError("Could not read " + file, e);
        }
    }
}
