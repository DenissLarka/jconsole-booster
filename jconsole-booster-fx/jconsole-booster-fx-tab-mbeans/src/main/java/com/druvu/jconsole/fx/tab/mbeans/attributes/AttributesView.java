package com.druvu.jconsole.fx.tab.mbeans.attributes;

import com.druvu.jconsole.core.mbeans.MBeanValues;
import com.druvu.jconsole.core.text.Messages;
import com.druvu.jconsole.fx.tab.mbeans.common.Failures;
import com.druvu.jconsole.fx.tab.mbeans.common.TabServices;
import com.druvu.jconsole.fx.tab.mbeans.common.TableCopy;
import com.druvu.jconsole.fx.tab.mbeans.viewers.ValueWindow;
import java.util.List;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javax.management.Attribute;
import javax.management.MBeanAttributeInfo;
import javax.management.ObjectName;

/**
 * The Name/Value table of an MBean's attributes, all of them or one, with the Refresh button. Values are read off the
 * FX thread; a writable value is set through the connection and the table read again.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
public final class AttributesView {

    private final TabServices services;
    private final ObjectName name;
    private final List<MBeanAttributeInfo> attributes;
    private final TableView<AttributeValue> table = new TableView<>();
    private final VBox pane;

    public AttributesView(TabServices services, ObjectName name, List<MBeanAttributeInfo> attributes) {
        this.services = services;
        this.name = name;
        this.attributes = List.copyOf(attributes);

        TableColumn<AttributeValue, String> nameColumn = new TableColumn<>(Messages.NAME);
        nameColumn.setCellValueFactory(
                cell -> new ReadOnlyStringWrapper(cell.getValue().name()));
        nameColumn.setCellFactory(column -> new NameCell());
        nameColumn.setPrefWidth(220);
        TableColumn<AttributeValue, String> valueColumn = new TableColumn<>(Messages.VALUE);
        valueColumn.setCellValueFactory(
                cell -> new ReadOnlyStringWrapper(cell.getValue().text()));
        valueColumn.setCellFactory(column -> new AttributeValueCell(this::write, this::view));
        table.getColumns().setAll(List.of(nameColumn, valueColumn));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setEditable(true);
        TableCopy.install(table);

        Button refresh = new Button(Messages.MBEANS_TAB_REFRESH_ATTRIBUTES_BUTTON);
        refresh.setTooltip(new Tooltip(Messages.MBEANS_TAB_REFRESH_ATTRIBUTES_BUTTON_TOOLTIP));
        refresh.setOnAction(event -> load());
        HBox buttons = new HBox(refresh);
        buttons.setAlignment(Pos.CENTER);
        buttons.setPadding(new Insets(6, 0, 0, 0));
        pane = new VBox(table, buttons);
        VBox.setVgrow(table, Priority.ALWAYS);
    }

    public Node node() {
        return pane;
    }

    /** The rows on screen, for tests. */
    List<AttributeValue> rows() {
        return table.getItems();
    }

    /** Reads the values again and shows them. */
    public void load() {
        services.load(
                "Read the attributes of " + name,
                () -> AttributeReader.read(services.access(), name, attributes),
                table.getItems()::setAll,
                Messages.PROBLEM_DISPLAYING_MBEAN);
    }

    /** Sets the attribute to the typed text through the connection, then reads the table again. */
    private void write(AttributeValue row, String text) {
        Object value;
        try {
            value = MBeanValues.createObjectFromString(row.info().getType(), text);
        } catch (ReflectiveOperationException | RuntimeException e) {
            services.host().showError(Messages.PROBLEM_SETTING_ATTRIBUTE, Failures.cause(e));
            return;
        }
        Attribute attribute = new Attribute(row.name(), value);
        services.load(
                "Set " + row.name() + " of " + name,
                () -> {
                    services.access().writeAttribute(name, attribute);
                    return attribute;
                },
                written -> load(),
                Messages.PROBLEM_SETTING_ATTRIBUTE);
    }

    private void view(AttributeValue row) {
        ValueWindow.show(services.host(), row.name(), row.value());
    }

    /** The attribute's name, its description as the tooltip. */
    private static final class NameCell extends TableCell<AttributeValue, String> {

        private final Tooltip tooltip = new Tooltip();

        @Override
        protected void updateItem(String text, boolean empty) {
            super.updateItem(text, empty);
            setText(empty ? null : text);
            int index = getIndex();
            boolean hasRow =
                    !empty && index >= 0 && index < getTableView().getItems().size();
            String description =
                    hasRow ? getTableView().getItems().get(index).info().getDescription() : null;
            tooltip.setText(description);
            setTooltip(description == null || description.isEmpty() ? null : tooltip);
        }
    }
}
