/*
 * Copyright (c) 2021 - Yupiik SAS - https://www.yupiik.com
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.yupiik.batch.runtime.iterator;

import io.yupiik.batch.runtime.batch.builder.BatchChain;

import java.util.Comparator;
import java.util.Iterator;
import java.util.function.Function;
import java.util.function.Predicate;

// stream like API but iterator oriented with less overhead
public class FluentIterator<A> implements Iterator<A>, BatchChain.Commentifiable, AutoCloseable {
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

    public CommentedIterator<A> withComment(final String comment) {
        return new CommentedIterator<>(comment, delegate);
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

    @Override
    public void close() throws Exception {
        if (AutoCloseable.class.isInstance(delegate)) {
            AutoCloseable.class.cast(delegate).close();
        }
    }

    public static class CommentedIterator<A> implements Iterator<A>, BatchChain.Commentifiable, AutoCloseable {
        private final String comment;
        private final Iterator<A> delegate;

        public CommentedIterator(final String comment, final Iterator<A> delegate) {
            this.comment = comment;
            this.delegate = delegate;
        }

        @Override
        public boolean hasNext() {
            return delegate.hasNext();
        }

        @Override
        public A next() {
            return delegate.next();
        }

        @Override
        public String toComment() {
            return comment;
        }

        @Override
        public void close() throws Exception {
            if (AutoCloseable.class.isInstance(delegate)) {
                AutoCloseable.class.cast(delegate).close();
            }
        }
    }
}
