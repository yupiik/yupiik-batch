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
import java.util.function.Function;

public class FlatMappingIterator<A, B> implements Iterator<B>, AutoCloseable {
    private final Iterator<A> delegate;
    private final Function<A, Iterator<B>> function;
    private Iterator<B> current;

    public FlatMappingIterator(final Iterator<A> delegate, final Function<A, Iterator<B>> function) {
        this.delegate = delegate;
        this.function = function;
    }

    @Override
    public boolean hasNext() {
        while (current == null || !current.hasNext()) {
            if (!delegate.hasNext()) {
                return false;
            }
            current = function.apply(delegate.next());
        }
        return true;
    }

    @Override
    public B next() {
        return current.next();
    }

    @Override
    public void close() throws Exception {
        if (AutoCloseable.class.isInstance(delegate)) {
            AutoCloseable.class.cast(delegate).close();
        }
    }
}
