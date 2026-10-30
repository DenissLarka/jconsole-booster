package com.druvu.jconsole.fx.tab.mbeans.notifications;

import com.druvu.jconsole.core.text.Messages;
import com.druvu.jconsole.fx.tab.mbeans.common.TabServices;
import com.druvu.jconsole.fx.tab.mbeans.common.TableCopy;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javax.management.ObjectName;

/**
 * An MBean's notification buffer, newest first, with Subscribe, Unsubscribe and Clear.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
public final class NotificationsView {

    private final TabServices services;
    private final NotificationBuffer buffer;
    private final ObjectName name;
    private final Consumer<ObjectName> changed;
    private final TableView<NotificationRow> table = new TableView<>();
    private final VBox pane;

    /** @param changed told when the buffer of an MBean changed, for the tree's label */
    public NotificationsView(
            TabServices services, NotificationBuffer buffer, ObjectName name, Consumer<ObjectName> changed) {
        this.services = services;
        this.buffer = buffer;
        this.name = name;
        this.changed = changed;
        table.getColumns()
                .setAll(List.of(
                        column(Messages.TIME_STAMP, NotificationRow::timeStamp, 110),
                        column(Messages.TYPE, NotificationRow::type, 200),
                        column(Messages.SEQ_NUM, NotificationRow::sequenceNumber, 70),
                        column(Messages.MESSAGE, NotificationRow::message, 400)));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        TableCopy.install(table);
        HBox buttons = new HBox(
                6,
                button(
                        Messages.MBEANS_TAB_SUBSCRIBE_NOTIFICATIONS_BUTTON,
                        Messages.MBEANS_TAB_SUBSCRIBE_NOTIFICATIONS_BUTTON_TOOLTIP,
                        this::subscribe),
                button(
                        Messages.MBEANS_TAB_UNSUBSCRIBE_NOTIFICATIONS_BUTTON,
                        Messages.MBEANS_TAB_UNSUBSCRIBE_NOTIFICATIONS_BUTTON_TOOLTIP,
                        this::unsubscribe),
                button(
                        Messages.MBEANS_TAB_CLEAR_NOTIFICATIONS_BUTTON,
                        Messages.MBEANS_TAB_CLEAR_NOTIFICATIONS_BUTTON_TOOLTIP,
                        this::clear));
        buttons.setAlignment(Pos.CENTER);
        buttons.setPadding(new Insets(6, 0, 0, 0));
        pane = new VBox(table, buttons);
        VBox.setVgrow(table, Priority.ALWAYS);
        refresh();
    }

    public Node node() {
        return pane;
    }

    /** Whether the view shows the MBean's buffer. */
    public boolean shows(ObjectName mbean) {
        return name.equals(mbean);
    }

    /** Shows the buffer as it is now. */
    public void refresh() {
        table.getItems().setAll(buffer.rows(name));
    }

    private void subscribe() {
        services.load(
                "Subscribe to " + name,
                () -> {
                    buffer.subscribe(name);
                    return name;
                },
                this::changedNow,
                Messages.PROBLEM_ADDING_LISTENER);
    }

    private void unsubscribe() {
        services.load(
                "Unsubscribe from " + name,
                () -> {
                    buffer.unsubscribe(name);
                    return name;
                },
                this::changedNow,
                Messages.PROBLEM_REMOVING_LISTENER);
    }

    private void clear() {
        buffer.clear(name);
        changedNow(name);
    }

    private void changedNow(ObjectName mbean) {
        refresh();
        changed.accept(mbean);
    }

    private static <T> TableColumn<NotificationRow, T> column(
            String title, Function<NotificationRow, T> value, double width) {
        TableColumn<NotificationRow, T> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(value.apply(cell.getValue())));
        column.setPrefWidth(width);
        column.setSortable(false);
        return column;
    }

    private static Button button(String text, String tooltip, Runnable action) {
        Button button = new Button(text);
        button.setTooltip(new Tooltip(tooltip));
        button.setOnAction(event -> action.run());
        return button;
    }
}
