package com.druvu.jconsole.fx.tab.mbeans.info;

import java.util.List;

/**
 * What the info view shows for one MBean, attribute, operation or notification.
 *
 * @param title the kind of info ({@code MBeanInfo}, {@code MBeanAttributeInfo}...)
 * @param rows the properties block by block, then the descriptor section when any descriptor has fields
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
public record InfoTable(String title, List<InfoRow> rows) {

    public InfoTable {
        rows = List.copyOf(rows);
    }
}
