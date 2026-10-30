package com.druvu.jconsole.fx.tab.mbeans.attributes;

import com.druvu.jconsole.core.mbeans.MBeanValues;
import com.druvu.jconsole.core.text.Messages;
import com.druvu.jconsole.fx.tab.mbeans.viewers.ValueLevel;
import com.druvu.jconsole.fx.tab.mbeans.viewers.ValueText;
import javax.management.MBeanAttributeInfo;

/**
 * One row of the attribute table: the attribute, the value read, or why it could not be read.
 *
 * @param info the attribute's description
 * @param value the value read; {@code null} when unreadable or unavailable
 * @param failure why the read failed, empty when it worked
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
public record AttributeValue(MBeanAttributeInfo info, Object value, String failure) {

    public String name() {
        return info.getName();
    }

    public boolean isAvailable() {
        return failure.isEmpty();
    }

    /** Whether the user may type a new value: readable, writable, and of a type made from text. */
    public boolean isEditable() {
        return isAvailable() && info.isWritable() && MBeanValues.isEditableType(info.getType());
    }

    /** Whether the value opens in the viewer: arrays, collections, composite and tabular data. */
    public boolean isViewable() {
        return isAvailable() && ValueLevel.of(value).isPresent();
    }

    /** What the table cell shows. */
    public String text() {
        return isAvailable() ? ValueText.summary(value) : Messages.UNAVAILABLE;
    }
}
