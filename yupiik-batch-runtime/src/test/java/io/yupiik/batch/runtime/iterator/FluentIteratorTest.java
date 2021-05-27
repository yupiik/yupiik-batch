package io.yupiik.batch.runtime.iterator;

import org.junit.jupiter.api.Test;

import java.util.List;

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
