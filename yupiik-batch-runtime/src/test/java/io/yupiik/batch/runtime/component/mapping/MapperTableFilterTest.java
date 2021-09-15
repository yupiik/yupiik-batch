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
package io.yupiik.batch.runtime.component.mapping;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapperTableFilterTest {
    @Test
    void filter() {
        final var filter = new MapperTableFilter(Spec.class, "test");
        assertTrue(filter.test("foo"));
        assertTrue(filter.test("dummy"));
        assertFalse(filter.test("dummy2"));
        assertFalse(filter.test("bar"));
        assertFalse(filter.test("true"));
        assertFalse(filter.test("whatever"));
    }

    @Mapping(to = Record.class, tables = @Mapping.MappingTable(name = "test", entries = {
            @Mapping.Entry(input = "foo", output = "bar"),
            @Mapping.Entry(input = "dummy", output = "true")
    }))
    public static class Spec {
    }
}
