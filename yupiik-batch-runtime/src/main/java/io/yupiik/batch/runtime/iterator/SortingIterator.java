package io.yupiik.batch.runtime.iterator;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.StreamSupport;

public class SortingIterator<A> implements Iterator<A> {
    private final Iterator<A> delegate;

    public SortingIterator(final Iterator<A> input, final Comparator<A> comparator) {
        var baseStream = StreamSupport.stream(Spliterators.spliteratorUnknownSize(input, Spliterator.IMMUTABLE), false);
        this.delegate = baseStream
                .sorted(comparator)
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
