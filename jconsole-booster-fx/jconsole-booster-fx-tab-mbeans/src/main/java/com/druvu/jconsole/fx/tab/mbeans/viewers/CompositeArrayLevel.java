package com.druvu.jconsole.fx.tab.mbeans.viewers;

import com.druvu.jconsole.core.text.Messages;
import java.util.List;
import java.util.Objects;
import javax.management.openmbean.CompositeData;

/**
 * An array of composites, shown one element at a time with previous and next.
 *
 * @param elements the composites, at least one
 * @param index the element shown
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
public record CompositeArrayLevel(List<CompositeData> elements, int index) implements ValueLevel {

    public CompositeArrayLevel {
        elements = List.copyOf(elements);
        Objects.checkIndex(index, elements.size());
    }

    @Override
    public String title() {
        return elements.getFirst().getCompositeType().getTypeName() + "[" + elements.size() + "]";
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public List<String> columns() {
        return List.of(Messages.NAME, Messages.VALUE);
    }

    @Override
    public List<List<Object>> rows() {
        return new CompositeLevel(elements.get(index)).rows();
    }

    public boolean hasPrevious() {
        return index > 0;
    }

    public boolean hasNext() {
        return index < elements.size() - 1;
    }

    public CompositeArrayLevel previous() {
        return new CompositeArrayLevel(elements, index - 1);
    }

    public CompositeArrayLevel next() {
        return new CompositeArrayLevel(elements, index + 1);
    }
}
