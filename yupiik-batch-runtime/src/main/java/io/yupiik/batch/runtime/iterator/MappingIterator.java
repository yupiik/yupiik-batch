package io.yupiik.batch.runtime.iterator;

import java.util.Iterator;
import java.util.function.Function;

public class MappingIterator<A, B> implements Iterator<B> {
    private final Function<A, B> function;
    private final Iterator<A> delegate;
    private A next;

    public MappingIterator(final Iterator<A> delegate, final Function<A, B> function) {
        this.function = function;
        this.delegate = delegate;
    }

    @Override
    public boolean hasNext() {
        return delegate.hasNext();
    }

    @Override
    public B next() {
        return function.apply(delegate.next());
    }
}
