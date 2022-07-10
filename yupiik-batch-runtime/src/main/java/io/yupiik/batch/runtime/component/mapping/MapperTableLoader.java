/*
 * Copyright (c) 2021-2022 - Yupiik SAS - https://www.yupiik.com
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

import java.io.IOException;
import java.io.StringReader;
import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Properties;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.util.Locale.ROOT;
import static java.util.stream.Collectors.toMap;

public class MapperTableLoader {
    private MapperTableLoader() {
        // no-op
    }

    public static Map<String, Map<String, String>> collectMappingTables(final String prefix,
                                                                        final Mapping conf,
                                                                        final Predicate<String> filter) {
        final var actualPrefix = prefix.replace('$', '.') + '.';
        return Stream.of(conf.tables())
                .filter(it -> filter.test(it.name()))
                .collect(toMap(Mapping.MappingTable::name, mt -> loadEntries(actualPrefix + mt.name(), mt.entries())
                        .collect(toMap(Mapping.Entry::input, Mapping.Entry::output))));
    }

    private static <T> Stream<Mapping.Entry> loadEntries(final String prefix, final Mapping.Entry[] defaultValue) {
        {// 1. try system properties, value is a properties one
            final var value = System.getProperty(prefix);
            if (value != null) {
                return loadEntriesFromProperties(value);
            }
        }
        // 2. try env var, value is a properties one
        final var value = System.getenv(prefix.replaceAll("[^A-Za-z0-9]", "_").toUpperCase(ROOT));
        if (value != null) {
            return loadEntriesFromProperties(value);
        }
        // 3. fallback on the default hardcoded value
        return Stream.of(defaultValue);
    }

    private static Stream<Mapping.Entry> loadEntriesFromProperties(final String value) {
        final var props = new Properties();
        try (final var reader = new StringReader(value)) {
            props.load(reader);
        } catch (final IOException e) {
            throw new IllegalArgumentException(e);
        }
        return props.stringPropertyNames().stream()
                .map(it -> new EntryImpl(it, props.getProperty(it)));
    }

    private static record EntryImpl(String input, String output) implements Mapping.Entry {
        @Override
        public Class<? extends Annotation> annotationType() {
            return Mapping.Entry.class;
        }
    }
}
