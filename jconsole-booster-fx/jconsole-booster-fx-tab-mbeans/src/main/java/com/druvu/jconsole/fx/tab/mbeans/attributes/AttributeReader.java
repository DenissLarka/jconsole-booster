package com.druvu.jconsole.fx.tab.mbeans.attributes;

import com.druvu.jconsole.core.mbeans.MBeanExceptions;
import com.druvu.jconsole.fx.tab.mbeans.common.MBeanAccess;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.management.Attribute;
import javax.management.JMException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanFeatureInfo;
import javax.management.ObjectName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads an MBean's attribute values in one call, then one by one the attributes that call left out, so a single failing
 * getter costs only its own row. Blocks: never on the FX thread.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
public final class AttributeReader {

    private static final Logger log = LoggerFactory.getLogger(AttributeReader.class);

    private AttributeReader() {}

    /** The values in the order of {@code attributes}; a readable attribute that fails says why. */
    public static List<AttributeValue> read(MBeanAccess access, ObjectName name, List<MBeanAttributeInfo> attributes) {
        Map<String, Object> values = new HashMap<>();
        Map<String, String> failures = new HashMap<>();
        String[] names = attributes.stream().map(MBeanFeatureInfo::getName).toArray(String[]::new);
        try {
            for (Attribute attribute : access.readAttributes(name, names).asList()) {
                values.put(attribute.getName(), attribute.getValue());
            }
        } catch (JMException | IOException | RuntimeException e) {
            log.debug("getAttributes failed on {}, reading the attributes one by one", name, e);
        }
        for (MBeanAttributeInfo attribute : attributes) {
            if (attribute.isReadable() && !values.containsKey(attribute.getName())) {
                try {
                    values.put(attribute.getName(), access.readAttribute(name, attribute.getName()));
                } catch (JMException | IOException | RuntimeException e) {
                    failures.put(
                            attribute.getName(), MBeanExceptions.actualCause(e).toString());
                }
            }
        }
        return attributes.stream()
                .map(attribute -> new AttributeValue(
                        attribute, values.get(attribute.getName()), failures.getOrDefault(attribute.getName(), "")))
                .toList();
    }
}
