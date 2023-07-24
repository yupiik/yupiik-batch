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

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SimpleIteratorTest {
    @Test
    void run() {
        final var counter = new AtomicInteger(3);
        assertEquals(List.of(1, 2, 3),
                StreamSupport.stream(Spliterators.spliteratorUnknownSize(
                                        new SimpleIterator<>(() -> counter.getAndDecrement() > 0, () -> 3 - counter.get()),
                                        Spliterator.IMMUTABLE),
                                false)
                        .toList());
    }
}
