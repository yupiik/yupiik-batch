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
