package io.yupiik.batch.runtime.component;

import io.yupiik.batch.runtime.component.diff.Diff;
import io.yupiik.batch.runtime.iterator.CountingIterator;

import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;

// highly inspired from
// https://github.com/rmannibucau/comparator/blob/master/src/main/java/com/github/rmannibucau/comparator/DifferenceAnalyzer.java#L22
public class DatasetDiffComputer<T> implements BiFunction<Iterator<T>, Iterator<T>, Diff<T>> {
    private final Comparator<T> keyComparator;
    private final BiPredicate<T, T> equalTester;

    public DatasetDiffComputer(final Comparator<T> keyComparator, final BiPredicate<T, T> equalTester) {
        this.keyComparator = keyComparator;
        this.equalTester = equalTester;
    }

    @Override
    public Diff<T> apply(final Iterator<T> rawIncoming, final Iterator<T> reference) {
        final var incoming = new CountingIterator<>(rawIncoming);

        final var missing = new LinkedList<T>();
        final var added = new LinkedList<T>();
        final var updated = new LinkedList<T>();

        T existingData = null;
        T newData = null;
        boolean oneMoreIteration = reference.hasNext() && incoming.hasNext();

        if (oneMoreIteration) {
            existingData = reference.next();
            newData = incoming.next();
        }

        while (oneMoreIteration) {
            oneMoreIteration = false;

            final int diff = keyComparator.compare(existingData, newData);
            if (diff > 0) {
                added.add(mapAdd(newData));
                oneMoreIteration = incoming.hasNext();
                if (oneMoreIteration) {
                    newData = incoming.next();
                } else {
                    missing.add(mapMiss(existingData));
                }
            }
            if (diff < 0) {
                missing.add(mapMiss(existingData));
                oneMoreIteration = reference.hasNext();
                if (oneMoreIteration) {
                    existingData = reference.next();
                } else {
                    added.add(mapAdd(newData));
                }
            }
            if (diff == 0) {
                if (!equalTester.test(existingData, newData)) {
                    updated.add(mapUpdate(existingData, newData));
                } // else no diff
                oneMoreIteration = reference.hasNext() && incoming.hasNext();
                if (oneMoreIteration) {
                    existingData = reference.next();
                    newData = incoming.next();
                }
            }
        }
        while (incoming.hasNext()) {
            added.add(mapAdd(incoming.next()));
        }
        while (reference.hasNext()) {
            missing.add(mapMiss(reference.next()));
        }

        return new Diff<>(missing, added, updated, incoming.getTotal());
    }

    protected T mapMiss(final T data) {
        return data;
    }

    protected T mapAdd(final T data) {
        return data;
    }

    protected T mapUpdate(final T previous, final T current) {
        return current;
    }
}
