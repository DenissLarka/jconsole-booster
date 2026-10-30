package com.druvu.jconsole.fx.tab.mbeans.info;

import com.druvu.jconsole.core.mbeans.MBeanValues;
import com.druvu.jconsole.core.text.Messages;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.management.Descriptor;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanConstructorInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.ObjectName;

/**
 * Turns JMX's descriptions into the rows of the info view: the feature's properties in one table, the descriptors in
 * the other, each block under a header.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
public final class InfoRows {

    private final List<InfoRow> info = new ArrayList<>();
    private final List<InfoRow> descriptor = new ArrayList<>();

    private InfoRows() {}

    public static InfoTables forMBean(ObjectName name, MBeanInfo mbean) {
        InfoRows rows = new InfoRows();
        String header = Messages.INFO + ":";
        rows.info.add(InfoRow.header(header));
        rows.info.add(InfoRow.of(Messages.OBJECT_NAME, name));
        rows.info.add(InfoRow.of(Messages.CLASS_NAME, mbean.getClassName()));
        rows.info.add(InfoRow.of(Messages.DESCRIPTION, mbean.getDescription()));
        rows.addDescriptor(header, mbean.getDescriptor());
        MBeanConstructorInfo[] constructors = mbean.getConstructors();
        for (int i = 0; i < constructors.length; i++) {
            String constructorHeader = Messages.CONSTRUCTOR + "-" + i + ":";
            rows.info.add(InfoRow.header(constructorHeader));
            rows.info.add(InfoRow.of(Messages.NAME, constructors[i].getName()));
            rows.info.add(InfoRow.of(Messages.DESCRIPTION, constructors[i].getDescription()));
            rows.addDescriptor(constructorHeader, constructors[i].getDescriptor());
            MBeanParameterInfo[] parameters = constructors[i].getSignature();
            for (int j = 0; j < parameters.length; j++) {
                rows.addParameter(Messages.PARAMETER + "-" + i + "-" + j + ":", parameters[j]);
            }
        }
        return rows.tables(Messages.MBEAN_INFO);
    }

    public static InfoTables forAttribute(MBeanAttributeInfo attribute) {
        InfoRows rows = new InfoRows();
        String header = Messages.ATTRIBUTE + ":";
        rows.info.add(InfoRow.header(header));
        rows.info.add(InfoRow.of(Messages.NAME, attribute.getName()));
        rows.info.add(InfoRow.of(Messages.DESCRIPTION, attribute.getDescription()));
        rows.info.add(InfoRow.of(Messages.READABLE, attribute.isReadable()));
        rows.info.add(InfoRow.of(Messages.WRITABLE, attribute.isWritable()));
        rows.info.add(InfoRow.of(Messages.IS, attribute.isIs()));
        rows.info.add(InfoRow.of(Messages.TYPE, attribute.getType()));
        rows.addDescriptor(header, attribute.getDescriptor());
        return rows.tables(Messages.MBEAN_ATTRIBUTE_INFO);
    }

    public static InfoTables forOperation(MBeanOperationInfo operation) {
        InfoRows rows = new InfoRows();
        String header = Messages.OPERATION + ":";
        rows.info.add(InfoRow.header(header));
        rows.info.add(InfoRow.of(Messages.NAME, operation.getName()));
        rows.info.add(InfoRow.of(Messages.DESCRIPTION, operation.getDescription()));
        rows.info.add(InfoRow.of(Messages.IMPACT, impact(operation.getImpact())));
        rows.info.add(InfoRow.of(Messages.RETURN_TYPE, operation.getReturnType()));
        rows.addDescriptor(header, operation.getDescriptor());
        MBeanParameterInfo[] parameters = operation.getSignature();
        for (int i = 0; i < parameters.length; i++) {
            rows.addParameter(Messages.PARAMETER + "-" + i + ":", parameters[i]);
        }
        return rows.tables(Messages.MBEAN_OPERATION_INFO);
    }

    public static InfoTables forNotification(MBeanNotificationInfo notification) {
        InfoRows rows = new InfoRows();
        String header = Messages.NOTIFICATION + ":";
        rows.info.add(InfoRow.header(header));
        rows.info.add(InfoRow.of(Messages.NAME, notification.getName()));
        rows.info.add(InfoRow.of(Messages.DESCRIPTION, notification.getDescription()));
        rows.info.add(InfoRow.of(Messages.NOTIF_TYPES, Arrays.toString(notification.getNotifTypes())));
        rows.addDescriptor(header, notification.getDescriptor());
        return rows.tables(Messages.MBEAN_NOTIFICATION_INFO);
    }

    static String impact(int impact) {
        return switch (impact) {
            case MBeanOperationInfo.INFO -> Messages.INFO_CAPITALIZED;
            case MBeanOperationInfo.ACTION -> Messages.ACTION_CAPITALIZED;
            case MBeanOperationInfo.ACTION_INFO -> Messages.ACTION_INFO_CAPITALIZED;
            default -> Messages.UNKNOWN_CAPITALIZED;
        };
    }

    private void addParameter(String header, MBeanParameterInfo parameter) {
        info.add(InfoRow.header(header));
        info.add(InfoRow.of(Messages.NAME, parameter.getName()));
        info.add(InfoRow.of(Messages.DESCRIPTION, parameter.getDescription()));
        info.add(InfoRow.of(Messages.TYPE, parameter.getType()));
        addDescriptor(header, parameter.getDescriptor());
    }

    /** The descriptor's fields under the header of the feature they describe; nothing for an empty descriptor. */
    private void addDescriptor(String header, Descriptor fields) {
        if (fields == null || fields.getFieldNames().length == 0) {
            return;
        }
        descriptor.add(InfoRow.header(header));
        for (String field : fields.getFieldNames()) {
            descriptor.add(InfoRow.of(field, MBeanValues.arrayToString(fields.getFieldValue(field))));
        }
    }

    private InfoTables tables(String title) {
        return new InfoTables(title, info, descriptor);
    }
}
