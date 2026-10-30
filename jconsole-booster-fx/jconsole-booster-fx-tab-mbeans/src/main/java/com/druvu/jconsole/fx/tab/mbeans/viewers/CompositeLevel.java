package com.druvu.jconsole.fx.tab.mbeans.viewers;

import com.druvu.jconsole.core.text.Messages;
import java.util.List;
import javax.management.openmbean.CompositeData;

/**
 * A composite value as Name/Value rows, one per item of its type.
 *
 * @param data the composite
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
public record CompositeLevel(CompositeData data) implements ValueLevel {

    @Override
    public String title() {
        return data.getCompositeType().getTypeName();
    }

    @Override
    public boolean isEmpty() {
        return data.getCompositeType().keySet().isEmpty();
    }

    @Override
    public List<String> columns() {
        return List.of(Messages.NAME, Messages.VALUE);
    }

    @Override
    public List<List<Object>> rows() {
        return data.getCompositeType().keySet().stream()
                .map(key -> List.<Object>of(key, ValueLevel.cell(data.get(key))))
                .toList();
    }
}
