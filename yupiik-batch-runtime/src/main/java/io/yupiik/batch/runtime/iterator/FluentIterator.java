package io.yupiik.batch.runtime.iterator;

import java.util.Comparator;
import java.util.Iterator;
import java.util.function.Function;
import java.util.function.Predicate;

public class FluentIterator<A> implements Iterator<A> {
    private final Iterator<A> delegate;

    private FluentIterator(final Iterator<A> delegate) {
        this.delegate = delegate;
    }

    public FluentIterator<A> filter(final Predicate<A> tester) {
        return new FluentIterator<>(new FilteringIterator<>(delegate, tester));
    }

    public <B> FluentIterator<B> map(final Function<A, B> function) {
        return new FluentIterator<>(new MappingIterator<>(delegate, function));
    }

    public FluentIterator<A> sort(final Comparator<A> comparator) {
        return new FluentIterator<>(new SortingIterator<>(delegate, comparator));
    }

    public <B> FluentIterator<A> distinct(final Function<A, B> keyExtractor, final Comparator<A> comparator) {
        return new FluentIterator<>(new DistinctIterator<>(delegate, keyExtractor, comparator));
    }

    public Iterator<A> unwrap() {
        return delegate;
    }

    public static <A> FluentIterator<A> of(final Iterator<A> it) {
        return new FluentIterator<>(it);
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
