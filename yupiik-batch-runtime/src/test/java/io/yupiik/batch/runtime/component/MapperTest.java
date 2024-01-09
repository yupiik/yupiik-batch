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
package io.yupiik.batch.runtime.component;

import io.yupiik.batch.runtime.component.mapping.Mapping;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MapperTest {
    @Test
    void simpleCompleteMapping() {
        assertEquals(
                new SimpleCopy("first", 123),
                new Mapper<>(SimpleMapperSpec.class).apply(new Simple("first", 123)));
    }

    @Test
    void simpleIncompleteMapping() {
        assertEquals(
                new SimpleCopy("first", 0),
                new Mapper<>(SimpleIncompleteMapperSpec.class).apply(new Simple("first", 123)));
    }

    @Test
    void computedMapping() {
        assertEquals(
                new SimpleCopy("first", 246),
                new Mapper<>(SimpleCustomMapperSpec.class).apply(new Simple("first", 123)));
    }

    @Test
    void tableMapping() {
        assertEquals(
                new SimpleCopy("premier", 0),
                new Mapper<>(SimpleTableSpec.class).apply(new Simple("first", 123)));
    }

    @Test
    void reversedTable() {
        assertEquals(
                new SimpleCopy("first", 0),
                new Mapper<>(ReversedTableSpec.class).apply(new Simple("first", 123)));
    }

    public static record Simple(String name, int age) {
    }

    public static record SimpleCopy(String name, int age) {
    }

    @Mapping(to = SimpleCopy.class, properties = {
            @Mapping.Property(from = "name", to = "name"),
            @Mapping.Property(from = "age", to = "age"),
    })
    public static record SimpleMapperSpec() {
    }

    @Mapping(to = SimpleCopy.class, properties = {
            @Mapping.Property(from = "name", to = "name")
    })
    public static record SimpleIncompleteMapperSpec() {
    }

    @Mapping(
            to = SimpleCopy.class,
            properties = @Mapping.Property(type = Mapping.PropertyType.TABLE_MAPPING, from = "name", to = "name", value = "m"),
            tables = @Mapping.MappingTable(name = "m", entries = @Mapping.Entry(input = "first", output = "premier")))
    public static record SimpleTableSpec() {
    }

    @Mapping(
            from = Simple.class,
            to = SimpleCopy.class,
            tables = @Mapping.MappingTable(name = "m", entries = @Mapping.Entry(input = "first", output = "premier")))
    public static record ReversedTableSpec() {
        @Mapping.Custom(description = "")
        String name(@Mapping.Table("m") final Mapping.ReversedTable table) {
            return table.get("premier").get(0);
        }
    }

    @Mapping(to = SimpleCopy.class, properties = {
            @Mapping.Property(from = "name", to = "name")
    })
    public static record SimpleCustomMapperSpec() {
        @Mapping.Custom(description = "Multiply the age by 2.")
        int age(final Simple simple) {
            return simple.age() * 2;
        }
    }
}
