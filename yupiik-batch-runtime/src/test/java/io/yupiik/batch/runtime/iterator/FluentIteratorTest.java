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
package io.yupiik.batch.runtime.iterator;

import org.junit.jupiter.api.Test;

import java.util.List;

import static java.util.Collections.emptyIterator;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FluentIteratorTest {
    @Test
    void filter() {
        final var iterator = FluentIterator.of(List.of("a", "b", "c", "d").iterator())
                .filter(it -> it.charAt(0) % 2 == 0)
                .unwrap();
        assertTrue(iterator.hasNext());
        assertEquals("b", iterator.next());
        assertTrue(iterator.hasNext());
        assertEquals("d", iterator.next());
        assertFalse(iterator.hasNext());
    }

    @Test
    void map() {
        final var iterator = FluentIterator.of(List.of("a", "bb").iterator())
                .map(String::length)
                .unwrap();
        assertTrue(iterator.hasNext());
        assertEquals(1, iterator.next());
        assertTrue(iterator.hasNext());
        assertEquals(2, iterator.next());
        assertFalse(iterator.hasNext());
    }

    @Test
    void flatMap() {
        final var iterator = FluentIterator.of(List.of("ab", "cd").iterator())
                .flatMap(it -> List.of(it.charAt(0), it.charAt(1)).iterator())
                .unwrap();
        assertTrue(iterator.hasNext());
        assertEquals('a', iterator.next());
        assertTrue(iterator.hasNext());
        assertEquals('b', iterator.next());
        assertTrue(iterator.hasNext());
        assertEquals('c', iterator.next());
        assertTrue(iterator.hasNext());
        assertEquals('d', iterator.next());
        assertFalse(iterator.hasNext());
    }

    @Test
    void flatMapWithEmpty() {
        final var iterator = FluentIterator.of(List.of("ab", "cd").iterator())
                .flatMap(it -> it.charAt(0) == 'a' ? emptyIterator() : List.of(it.charAt(0), it.charAt(1)).iterator())
                .unwrap();
        assertTrue(iterator.hasNext());
        assertEquals('c', iterator.next());
        assertTrue(iterator.hasNext());
        assertEquals('d', iterator.next());
        assertFalse(iterator.hasNext());
    }

    @Test
    void chain() {
        final var iterator = FluentIterator.of(List.of("a", "bb").iterator())
                .map(String::length)
                .filter(l -> l > 1)
                .unwrap();
        assertTrue(iterator.hasNext());
        assertEquals(2, iterator.next());
        assertFalse(iterator.hasNext());
    }
}
