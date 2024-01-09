/*
 * Copyright (c) 2021-present - Yupiik SAS - https://www.yupiik.com
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
package io.yupiik.batch.runtime.component.mapping;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;
import java.util.function.Predicate;

import static io.yupiik.batch.runtime.component.mapping.MapperTableLoader.collectMappingTables;
import static java.util.Optional.ofNullable;

public class MapperTableFilter implements Predicate<String> {
    private final Set<String> keys;

    public MapperTableFilter(final Class<?> mapperSpec,
                             final String tableName) {
        this(mapperSpec, tableName, true);
    }

    public MapperTableFilter(final Class<?> mapperSpec,
                             final String tableName,
                             final boolean useInput) {
        keys = ofNullable(collectMappingTables(mapperSpec.getName(), mapperSpec.getAnnotation(Mapping.class), tableName::equals))
                .map(map -> map.get(tableName))
                .map(it -> useInput ? it.keySet() : new HashSet<>(it.values()))
                .orElseThrow(() -> new IllegalArgumentException("No table '" + tableName + "' found on " + mapperSpec));
    }

    @Override
    public boolean test(final String value) {
        return keys.contains(value);
    }

    public <A> CountingPredicate<A> forField(final Function<A, String> extractor) {
        return new CountingPredicate<>(item -> test(extractor.apply(item)));
    }

    public <A> CountingPredicate<A> forList(final Function<A, List<String>> extractor) {
        return new CountingPredicate<>(item -> {
            final var list = extractor.apply(item);
            if (list != null) {
                return list.stream().anyMatch(MapperTableFilter.this);
            }
            return false;
        });
    }

    public static class CountingPredicate<A> implements Predicate<A> {
        private final LongAdder counter = new LongAdder();
        private final Predicate<A> filter;

        private CountingPredicate(final Predicate<A> filter) {
            this.filter = filter;
        }

        @Override
        public boolean test(final A a) {
            final var result = filter.test(a);
            if (!result) {
                counter.increment();
            }
            return result;
        }

        public long getCounter() {
            return counter.sum();
        }
    }
}
