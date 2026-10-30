package com.druvu.jconsole.fx.tab.mbeans.info;

import java.util.List;

/**
 * What the info view shows for one MBean, attribute, operation or notification.
 *
 * @param title the kind of info ({@code MBeanInfo}, {@code MBeanAttributeInfo}...)
 * @param info the info rows
 * @param descriptor the descriptor rows of the feature and its parts
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
public record InfoTables(String title, List<InfoRow> info, List<InfoRow> descriptor) {

    public InfoTables {
        info = List.copyOf(info);
        descriptor = List.copyOf(descriptor);
    }
}
