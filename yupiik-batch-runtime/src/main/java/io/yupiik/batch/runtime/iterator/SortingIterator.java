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
