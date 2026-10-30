package com.druvu.jconsole.fx.tab.mbeans.tree;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.management.ObjectName;

/**
 * Where an MBean sits in the tree: its domain, then one level per key property, {@code type} and {@code j2eeType} first
 * and the other keys in the order the name lists them. The last level is the MBean itself.
 *
 * @param steps the levels from the domain down to the MBean, never empty
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
public record MBeanPath(List<Step> steps) {

    /** The keys that lead the path when the name has them, in this order. */
    private static final List<String> LEADING_KEYS = List.of("type", "j2eeType");

    public MBeanPath {
        steps = List.copyOf(steps);
    }

    /**
     * One level of the tree.
     *
     * @param id the level's identity among all MBeans: the domain and the key properties down to this level
     * @param label what the tree shows: the domain, or the key's value
     * @param tooltip the domain, or {@code key=value}
     */
    public record Step(String id, String label, String tooltip) {}

    public static MBeanPath of(ObjectName name) {
        String domain = name.getDomain();
        List<Step> steps = new ArrayList<>();
        steps.add(new Step(domain, domain, domain));
        StringBuilder id = new StringBuilder(domain).append(':');
        for (Map.Entry<String, String> key : orderedKeys(name).entrySet()) {
            String token = key.getKey() + "=" + key.getValue();
            if (steps.size() > 1) {
                id.append(',');
            }
            id.append(token);
            steps.add(new Step(id.toString(), key.getValue(), token));
        }
        return new MBeanPath(steps);
    }

    /** The MBean's own level. */
    public Step leaf() {
        return steps.getLast();
    }

    /** The key properties in tree order. */
    static Map<String, String> orderedKeys(ObjectName name) {
        Map<String, String> listed = keysAsListed(name);
        Map<String, String> ordered = new LinkedHashMap<>();
        for (String key : LEADING_KEYS) {
            String value = listed.remove(key);
            if (value != null) {
                ordered.put(key, value);
            }
        }
        ordered.putAll(listed);
        return ordered;
    }

    /**
     * The key properties in the order the name was written with; {@link ObjectName#getKeyPropertyList()} loses it.
     * Values are taken from the name itself, so a quoted value holding {@code ,} or {@code =} is skipped whole.
     */
    private static Map<String, String> keysAsListed(ObjectName name) {
        Map<String, String> keys = new LinkedHashMap<>();
        String rest = name.getKeyPropertyListString();
        int equals = rest.indexOf('=');
        while (equals > 0) {
            String key = rest.substring(0, equals);
            String value = name.getKeyProperty(key);
            if (value == null) {
                break;
            }
            keys.put(key, value);
            rest = rest.substring(Math.min(rest.length(), equals + 1 + value.length()));
            if (rest.startsWith(",")) {
                rest = rest.substring(1);
            }
            equals = rest.indexOf('=');
        }
        return keys;
    }
}
