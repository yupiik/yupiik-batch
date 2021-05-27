package io.yupiik.batch.runtime.iterator;

import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

// todo: better impl in terms of mem
public class DistinctIterator<A, B> implements Iterator<A> {
    private final Logger logger = Logger.getLogger(getClass().getName());

    private final Iterator<A> delegate;

    public DistinctIterator(final Iterator<A> delegate, final Function<A, B> keyExtractor, final Comparator<A> comparator) {
        final var data = StreamSupport.stream(Spliterators.spliteratorUnknownSize(delegate, Spliterator.IMMUTABLE), false)
                .collect(groupingBy(keyExtractor, LinkedHashMap::new, toList())); // keep the order
        this.delegate = data.values().stream()
                .map(l -> {
                    if (l.size() == 1) {
                        return l.get(0);
                    }
                    l.sort(comparator);
                    logger.info(() -> "Discarding " + l.subList(1, l.size()) + " in favor of " + l.get(0));
                    return l.get(0);
                })
                .iterator();
    }

    @Override
    public boolean hasNext() {
        return delegate.hasNext();
    }

    @Override
    public A next() {
        return delegate.next();
    }
}
