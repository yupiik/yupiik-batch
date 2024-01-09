/*
 * Copyright (c) 2021-present - Yupiik SAS - https://www.yupiik.com
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

import io.yupiik.batch.runtime.tracing.BaseExecutionTracer;
import io.yupiik.batch.runtime.batch.internal.BatchPromiseImpl;

import java.util.concurrent.CompletionStage;

/**
 * Using the {@link io.yupiik.batch.runtime.batch.builder.BatchChain} you can return this wrapper
 * in a step which is traced thanks a tracer
 * (see {@link io.yupiik.batch.runtime.batch.builder.RunConfiguration} and {@link BaseExecutionTracer}).
 *
 * This enables steps to await asynchronously previous one and start processing its results before it fully ends
 * but keeps the tracing accurate.
 *
 * The tip is to use a reactive value (end value just being a callback for the tracer to know when the step is actually done).
 *
 * @param <A>
 */
public interface BatchPromise<A> {
    /**
     * @return value this promise transmits to the next step.
     */
    A value();

    /**
     * @return callback to know when the step ends.
     */
    CompletionStage<Void> end();

    /**
     * Creates a {@link BatchPromise}.
     * @param value the value.
     * @param endPromise the end callback.
     * @return a built BatchPromise.
     * @param <A> the v
     *           alue type.
     */
    static <A> BatchPromise<A> of(final A value, final CompletionStage<Void> endPromise) {
        return new BatchPromiseImpl<>(value, endPromise);
    }
}
