package com.druvu.jconsole.fx.charts;

import com.druvu.jconsole.core.text.Messages;
import javafx.beans.property.ObjectProperty;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.ToggleGroup;

/**
 * A chart's context menu: the time range as radio items, then "Save data as...".
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
final class ChartContextMenu {

    private ChartContextMenu() {}

    static ContextMenu create(ObjectProperty<TimeRange> timeRange, Runnable saveDataAs) {
        ToggleGroup ranges = new ToggleGroup();
        Menu rangeMenu = new Menu(Messages.PLOTTER_TIME_RANGE_MENU);
        for (TimeRange range : TimeRange.values()) {
            RadioMenuItem item = new RadioMenuItem(range.label());
            item.setToggleGroup(ranges);
            item.setUserData(range);
            item.setSelected(range == timeRange.get());
            item.setOnAction(event -> timeRange.set(range));
            rangeMenu.getItems().add(item);
        }
        // the range also changes from a chooser or another chart's menu: keep the mark on the range shown
        timeRange.addListener((observable, previous, current) ->
                ranges.getToggles().forEach(toggle -> toggle.setSelected(toggle.getUserData() == current)));

        MenuItem saveAs = new MenuItem(Messages.PLOTTER_SAVE_AS_MENU_ITEM);
        saveAs.setOnAction(event -> saveDataAs.run());
        return new ContextMenu(rangeMenu, new SeparatorMenuItem(), saveAs);
    }
}
