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

import java.util.Iterator;
import java.util.function.Predicate;

public class FilteringIterator<A> implements Iterator<A>, AutoCloseable {
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

    @Override
    public void close() throws Exception {
        if (AutoCloseable.class.isInstance(delegate)) {
            AutoCloseable.class.cast(delegate).close();
        }
    }
}
