package com.druvu.jconsole.fx.tab.jtop;

import java.util.function.Function;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;

/**
 * The JTop table and its status line. The rows arrive busiest first; once the user sorts by a column, every new reading
 * is shown in the user's order instead. Built and used on the FX thread only.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
final class JTopView {

    /** The style class of the status line under the table. */
    static final String STATUS_STYLE_CLASS = "jtop-status";

    private final ObservableList<ThreadRow> rows = FXCollections.observableArrayList();
    private final Label status = new Label();
    private final BorderPane root = new BorderPane();

    JTopView() {
        TableColumn<ThreadRow, String> name = column("ThreadName", ThreadRow::name);
        name.setPrefWidth(320);
        TableColumn<ThreadRow, Double> cpu = column("CPU(sec)", ThreadRow::cpuSeconds);
        cpu.setCellFactory(column -> new CpuSecondsCell());
        cpu.setPrefWidth(110);
        TableColumn<ThreadRow, Thread.State> state = column("State", ThreadRow::state);
        state.setPrefWidth(130);

        TableView<ThreadRow> table = new TableView<>();
        table.getColumns().add(name);
        table.getColumns().add(cpu);
        table.getColumns().add(state);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setPlaceholder(new Label("No threads to show"));
        // The table sorts a sorted view of the rows, so the user's column order outlives every new reading.
        SortedList<ThreadRow> sorted = new SortedList<>(rows);
        sorted.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sorted);

        status.getStyleClass().add(STATUS_STYLE_CLASS);
        status.setPadding(new Insets(4, 8, 4, 8));
        root.setCenter(table);
        root.setBottom(status);
    }

    Node root() {
        return root;
    }

    /** Puts a reading on screen. */
    void show(ThreadSnapshot snapshot) {
        rows.setAll(snapshot.rows());
        status.setText(snapshot.status());
    }

    private static <T> TableColumn<ThreadRow, T> column(String title, Function<ThreadRow, T> value) {
        TableColumn<ThreadRow, T> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(value.apply(cell.getValue())));
        return column;
    }
}
