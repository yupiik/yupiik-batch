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

import java.util.Iterator;

public class CountingIterator<T> implements Iterator<T>, AutoCloseable {
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

    @Override
    public void close() throws Exception {
        if (AutoCloseable.class.isInstance(delegate)) {
            AutoCloseable.class.cast(delegate).close();
        }
    }
}
