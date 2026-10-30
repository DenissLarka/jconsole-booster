package com.druvu.jconsole.fx.tab.jtop;

import javafx.geometry.Pos;
import javafx.scene.control.TableCell;

/**
 * Shows a CPU time in seconds with four decimals, right-aligned so the digits line up.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
final class CpuSecondsCell extends TableCell<ThreadRow, Double> {

    CpuSecondsCell() {
        setAlignment(Pos.CENTER_RIGHT);
    }

    @Override
    protected void updateItem(Double seconds, boolean empty) {
        super.updateItem(seconds, empty);
        setText(empty || seconds == null ? null : text(seconds));
    }

    /** The CPU time as the cell shows it, in the user's locale. */
    static String text(double seconds) {
        return String.format("%.4f", seconds);
    }
}
