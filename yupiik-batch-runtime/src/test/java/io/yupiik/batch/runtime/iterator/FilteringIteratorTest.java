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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FilteringIteratorTest {
    @Test
    void filter() {
        final var src = List.of(0, 1, 2, 3);
        {
            final var filtered = new FilteringIterator<>(src.iterator(), i -> i % 2 == 0);
            assertTrue(filtered.hasNext());
            assertEquals(0, filtered.next());
            assertTrue(filtered.hasNext());
            assertEquals(2, filtered.next());
            assertFalse(filtered.hasNext());
            assertFalse(filtered.hasNext());
        }
        {
            final var filtered = new FilteringIterator<>(src.iterator(), i -> i % 2 == 1);
            assertTrue(filtered.hasNext());
            assertEquals(1, filtered.next());
            assertTrue(filtered.hasNext());
            assertEquals(3, filtered.next());
            assertFalse(filtered.hasNext());
            assertFalse(filtered.hasNext());
        }
    }
}
