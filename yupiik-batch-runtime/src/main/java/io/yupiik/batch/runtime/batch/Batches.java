package io.yupiik.batch.runtime.batch;

import java.lang.reflect.ParameterizedType;
import java.util.stream.Stream;

public final class Batches {
    private Batches() {
        // no-op
    }

    public static Class<?> findConfType(final Class<? extends Batch<?>> type) {
        return Stream.of(type.getGenericInterfaces())
                .filter(ParameterizedType.class::isInstance)
                .map(ParameterizedType.class::cast)
                .filter(pt -> pt.getRawType() == Batch.class)
                .findFirst()
                .map(pt -> Class.class.cast(pt.getActualTypeArguments()[0]))
                .orElseThrow(() -> new IllegalArgumentException("Ensure to implement Batch<Config> in " + type));
    }
}
