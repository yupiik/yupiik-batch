package io.yupiik.batch.runtime.iterator;

import java.util.Iterator;

public class CountingIterator<T> implements Iterator<T> {
    private final Iterator<T> delegate;
    private long total;

    public CountingIterator(final Iterator<T> delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean hasNext() {
        return delegate.hasNext();
    }

    @Override
    public T next() {
        final var next = delegate.next();
        total++;
        return next;
    }

    public long getTotal() {
        return total;
    }
}
