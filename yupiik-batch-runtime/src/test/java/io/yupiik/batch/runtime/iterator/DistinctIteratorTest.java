package io.yupiik.batch.runtime.iterator;

import org.junit.jupiter.api.Test;

import java.util.List;

import static java.util.Comparator.comparing;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DistinctIteratorTest {
    @Test
    void distinct() {
        final var iterator = FluentIterator.of(List.of("a", "aa", "b", "aaa").iterator())
                .distinct(s -> s.charAt(0), comparing(String::length).reversed())
                .unwrap();
        assertTrue(iterator.hasNext());
        assertEquals("aaa", iterator.next());
        assertTrue(iterator.hasNext());
        assertEquals("b", iterator.next());
        assertFalse(iterator.hasNext());
    }
}
