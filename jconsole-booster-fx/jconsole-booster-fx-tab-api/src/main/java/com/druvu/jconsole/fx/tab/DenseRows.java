package com.druvu.jconsole.fx.tab;

import javafx.scene.control.Control;

/**
 * The row height of the tabs' tables, lists and trees: AtlantaFX's dense size, rows of 2em instead of 3em and a 34 px
 * header, since a monitoring tool shows many rows at once.
 *
 * @author Deniss Larka <br>
 *     on 31 Oct 2026
 */
public final class DenseRows {

    /** AtlantaFX's {@code Styles.DENSE}, named here so the tab API stays free of AtlantaFX. */
    private static final String DENSE = "dense";

    private DenseRows() {}

    /** Gives {@code control}, a table, list, tree or tree table, the dense row height. */
    public static void apply(Control control) {
        control.getStyleClass().add(DENSE);
    }
}
