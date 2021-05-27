package io.yupiik.batch.runtime.iterator;

import java.util.Iterator;
import java.util.function.Predicate;

public class FilteringIterator<A> implements Iterator<A> {
    private final Predicate<A> filter;
    private final Iterator<A> delegate;
    private A next;

    public FilteringIterator(final Iterator<A> delegate, final Predicate<A> filter) {
        this.filter = filter;
        this.delegate = delegate;
    }

    @Override
    public boolean hasNext() {
        if (next != null) {
            return true;
        }
        while (delegate.hasNext()) {
            final var value = delegate.next();
            if (filter.test(value)) {
                next = value;
                return true;
            }
        }
        return false;
    }

    @Override
    public A next() {
        final var value = next;
        next = null;
        return value;
    }
}
