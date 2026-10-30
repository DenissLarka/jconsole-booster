package com.druvu.jconsole.fx.tab.memory;

import java.util.List;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;

/**
 * The details as a two-column grid: each name in bold, right-aligned, then its value. FX thread only.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
final class DetailsGrid extends GridPane {

    static final String STYLE_CLASS = "memory-details";

    DetailsGrid() {
        getStyleClass().add(STYLE_CLASS);
        setHgap(8);
        setVgap(2);
        setPadding(new Insets(4, 8, 4, 8));
    }

    /** Replaces the lines shown with {@code rows}. */
    void show(List<DetailRow> rows) {
        getChildren().clear();
        for (int i = 0; i < rows.size(); i++) {
            DetailRow row = rows.get(i);
            Label name = new Label(row.name().isEmpty() ? "" : row.name() + ":");
            name.setStyle("-fx-font-weight: bold;");
            setHalignment(name, HPos.RIGHT);
            addRow(i, name, new Label(row.value()));
        }
    }
}
