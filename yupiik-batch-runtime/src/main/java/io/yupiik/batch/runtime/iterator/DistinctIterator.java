/*
 * Copyright (c) 2021-2023 - Yupiik SAS - https://www.yupiik.com
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
import java.util.LinkedHashMap;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

// todo: better impl in terms of mem
public class DistinctIterator<A, B> implements Iterator<A>, AutoCloseable {
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

    @Override
    public void close() throws Exception {
        if (AutoCloseable.class.isInstance(delegate)) {
            AutoCloseable.class.cast(delegate).close();
        }
    }
}
