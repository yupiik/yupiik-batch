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
package io.yupiik.batch.runtime.batch;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;

public final class Batches {
    private Batches() {
        // no-op
    }

    public static Class<?> findConfType(final Class<? extends Batch<?>> type) {
        return findBatchInterface(type)
                .map(pt -> Class.class.cast(pt.getActualTypeArguments()[0]))
                .or(() -> ofNullable(type.getGenericSuperclass())
                        .filter(ParameterizedType.class::isInstance)
                        .map(ParameterizedType.class::cast)
                        .flatMap(it -> resolveSubClassConfig(type, it)))
                .orElseThrow(() -> new IllegalArgumentException("Ensure to implement Batch<Config> in " + type));
    }

    private static Optional<ParameterizedType> findBatchInterface(final Class<?> type) {
        return Stream.of(type.getGenericInterfaces())
                .filter(ParameterizedType.class::isInstance)
                .map(ParameterizedType.class::cast)
                .filter(pt -> pt.getRawType() == Batch.class)
                .findFirst();
    }

    private static Optional<Class<?>> resolveSubClassConfig(final Class<? extends Batch<?>> type, final ParameterizedType parameterizedType) {
        final var rawType = parameterizedType.getRawType();
        if (!Class.class.isInstance(rawType)) {
            return empty();
        }
        final var parent = Class.class.cast(rawType);
        if (Batch.class.isAssignableFrom(parent)) { // todo: recursive? for now accept one level
            final Optional<ParameterizedType> batchInterface = findBatchInterface(parent);
            return batchInterface.map(pt -> {
                final var conf = pt.getActualTypeArguments()[0];
                if (TypeVariable.class.isInstance(conf)) {
                    return Class.class.cast(resolveTypeVariable(conf, type));
                }
                if (Class.class.isInstance(conf)) {
                    return Class.class.cast(conf);
                }
                throw new IllegalArgumentException("Can't resolve " + conf + " from " + type);
            });
        }
        return empty();
    }

    private static Type resolveTypeVariable(final Type value, final Type rootClass) {
        final var tv = TypeVariable.class.cast(value);
        Type parent = rootClass;
        while (Class.class.isInstance(parent)) {
            parent = Class.class.cast(parent).getGenericSuperclass();
        }
        while (ParameterizedType.class.isInstance(parent) && ParameterizedType.class.cast(parent).getRawType() != tv.getGenericDeclaration()) {
            parent = Class.class.cast(ParameterizedType.class.cast(parent).getRawType()).getGenericSuperclass();
        }
        if (ParameterizedType.class.isInstance(parent)) {
            final var parentPt = ParameterizedType.class.cast(parent);
            final int argIndex = List.of(Class.class.cast(parentPt.getRawType()).getTypeParameters()).indexOf(tv);
            if (argIndex >= 0) {
                final var type = parentPt.getActualTypeArguments()[argIndex];
                if (TypeVariable.class.isInstance(type)) {
                    return resolveTypeVariable(type, rootClass);
                }
                return type;
            }
        }
        if (Class.class.isInstance(rootClass)) {
            return Object.class; // prefer a default over
        }
        return value;
    }
}
