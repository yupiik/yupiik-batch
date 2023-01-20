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
package io.yupiik.batch.runtime.batch;

import io.yupiik.batch.runtime.batch.builder.BatchChain;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public interface Batch<Conf> extends Consumer<Conf> {
    static void run(final Class<? extends Batch<?>> type, final String... args) {
        if (!type.isAnnotationPresent(BatchMeta.class)) {
            throw new IllegalArgumentException("Missing @BatchMeta on " + type);
        }
        final var confType = Batches.findConfType(type);
        final var configuration = new Binder(type.getSimpleName().toLowerCase(Locale.ROOT), List.of(args)).bind(confType);
        try {
            final Batch batch = type.getConstructor().newInstance();
            batch.accept(configuration);
        } catch (final InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }

    default BatchChain.BatchRoot<Void> from() {
        return new BatchChain.BatchRoot<>() {
            @Override
            public Result<Void> execute() {
                return new Result<>(null, Result.Type.CONTINUE);
            }

            @Override
            public boolean skipTracing() {
                return true;
            }

            @Override
            public String name() {
                return "root";
            }
        };
    }
}
