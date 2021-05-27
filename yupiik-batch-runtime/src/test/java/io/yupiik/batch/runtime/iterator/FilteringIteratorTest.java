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
