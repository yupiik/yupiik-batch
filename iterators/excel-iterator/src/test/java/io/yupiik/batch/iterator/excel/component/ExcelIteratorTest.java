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
package io.yupiik.batch.iterator.excel.component;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Paths;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.StreamSupport;

import static io.yupiik.batch.iterator.excel.component.ExcelIterator.ofLines;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ExcelIteratorTest {
    @ParameterizedTest
    @ValueSource(strings = {"xls", "xlsx"})
    void iterate(final String type) {
        try (final var it = ofLines(Paths.get("target/test-classes/test." + type), 0)) {
            assertEquals(
                    List.of(
                            List.of("c1", "c2", "c3"),
                            List.of("1", "2", "text1"),
                            List.of("2", "4", "text2"),
                            List.of("3", "6", "text3")
                    ),
                    StreamSupport.stream(Spliterators.spliteratorUnknownSize(it, Spliterator.IMMUTABLE), false)
                            .collect(toList())
            );
        }
    }
}
