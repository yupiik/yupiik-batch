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
package io.yupiik.batch.runtime.component;

import io.yupiik.batch.runtime.component.diff.Diff;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;
import java.util.function.BiPredicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatasetDiffComputerTest {
    @Test
    void noDiff() {
        final var diff = compare(
                List.of(new Person(1, "a"), new Person(2, "b"), new Person(3, "c")),
                List.of(new Person(1, "a"), new Person(2, "b"), new Person(3, "c")));
        assertTrue(diff.added().isEmpty());
        assertTrue(diff.deleted().isEmpty());
        assertTrue(diff.updated().isEmpty());
    }

    @Test
    void noInput() {
        final var diff = compare(
                List.of(),
                List.of(new Person(1, "a"), new Person(2, "b"), new Person(3, "c")));
        assertTrue(diff.added().isEmpty());
        assertEquals(3, diff.deleted().size());
        assertTrue(diff.updated().isEmpty());
    }

    @Test
    void noExisting() {
        final var diff = compare(
                List.of(new Person(1, "a"), new Person(2, "b"), new Person(3, "c")),
                List.of());
        assertEquals(3, diff.added().size());
        assertTrue(diff.deleted().isEmpty());
        assertTrue(diff.updated().isEmpty());
    }

    @Test
    void updated() {
        final var diff = compare(
                List.of(new Person(1, "a"), new Person(2, "bb"), new Person(3, "c")),
                List.of(new Person(1, "a"), new Person(2, "b"), new Person(3, "c")));
        assertTrue(diff.added().isEmpty());
        assertTrue(diff.deleted().isEmpty());
        assertEquals(1, diff.updated().size());
        assertEquals("bb", diff.updated().iterator().next().name);
    }

    @Test
    void fullWithMoreExistingThanNew() {
        final var diff = compare(
                List.of(new Person(2, "b"), new Person(3, "c"), new Person(4, "d")),
                List.of(new Person(1, "a"), new Person(2, "b"), new Person(3, "cc"), new Person(5, "e")));
        assertEquals(1, diff.added().size());
        assertEquals(4, diff.added().iterator().next().id);
        assertEquals(1, diff.updated().size());
        assertEquals(3, diff.updated().iterator().next().id);
        assertEquals(2, diff.deleted().size());
        final var missIt = diff.deleted().iterator();
        assertEquals(1, missIt.next().id);
        assertEquals(5, missIt.next().id);
    }

    @Test
    void missingDataAtTheEnd() {
        final var diff = compare(
                List.of(new Person(2, "b")),
                List.of(new Person(2, "b"), new Person(3, "c"), new Person(4, "d")));
        assertTrue(diff.added().isEmpty());
        assertEquals(2, diff.deleted().size());
        assertTrue(diff.updated().isEmpty());
    }

    @Test
    void missingDataAtTheBeginning() {
        final var diff = compare(
                List.of(new Person(2, "b"), new Person(3, "c"), new Person(4, "d")),
                List.of(new Person(2, "b")));
        assertEquals(2, diff.added().size());
        assertTrue(diff.deleted().isEmpty());
        assertTrue(diff.updated().isEmpty());
    }

    @Test
    void firstMissingAndNew() {
        final var diff = compare(
                List.of(new Person(2, "b"), new Person(3, "c"), new Person(4, "d")),
                List.of(new Person(1, "b")));
        assertEquals(3, diff.added().size());
        assertEquals(1, diff.deleted().size());
        assertTrue(diff.updated().isEmpty());
    }

    private Diff<Person> compare(final List<Person> d1,
                                 final List<Person> d2) {
        return new DatasetDiffComputer<>(new KeyComparator(), new BeanComparator()).apply(d1.iterator(), d2.iterator());
    }

    public record Person(long id, String name) {
    }

    public static class KeyComparator implements Comparator<Person> {
        @Override
        public int compare(final Person o1, final Person o2) {
            return (int) (o1.id - o2.id);
        }
    }

    public static class BeanComparator implements BiPredicate<Person, Person> {
        @Override
        public boolean test(final Person o1, final Person o2) {
            return o1.name.equals(o2.name);
        }
    }
}
