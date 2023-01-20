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
package io.yupiik.batch.runtime.batch.builder;

import io.yupiik.batch.runtime.batch.BatchPromise;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Logger;

import static java.util.Collections.reverse;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.logging.Level.SEVERE;

/**
 * Very trivial way to define common operations simply.
 * A more advanced fluent API could be inspired from Stream API but this one is sufficient for most data batches.
 *
 * @param <R> data type.
 */
public interface BatchChain<PP, P, R> extends Executable<P, R> {
    /**
     * Just an alias of {@code io.yupiik.batch.runtime.batch.builder.BatchChain} which forces previous to be empty.
     *
     * @param <B> data type.
     */
    interface BatchRoot<B> extends BatchChain<Void, Void, B> {
        Result<B> execute();

        @Override
        default Result<B> execute(final RunConfiguration configuration, final Result<Void> previous) {
            return execute();
        }

        @Override
        default Optional<BatchChain<?, Void, Void>> previous() {
            return Optional.empty();
        }
    }

    /**
     * @return the previous batch chain element in the chain.
     */
    Optional<BatchChain<?, PP, P>> previous();

    /**
     * @return the step name.
     */
    String name();

    /**
     * IMPORTANT: this is mainly for DSL dedicated method like {@code from()} which is a step doing nothing
     * so we don't care about its monitoring but it is needed for the fluent API.
     *
     * @return false to trace the step, true otherwise.
     */
    default boolean skipTracing() {
        return false;
    }

    default void run(final RunConfiguration configuration) { // todo: shouldn't we forbid configuration to be null?
        final var chain = new ArrayList<BatchChain<?, ?, ?>>();
        BatchChain<?, ?, ?> current = this;
        while (current != null) {
            chain.add(current);
            current = current.previous().orElse(null);
        }
        reverse(chain);

        if (!BatchRoot.class.isInstance(chain.get(0))) {
            throw new IllegalArgumentException(chain + " does not start with a BatchRoot, use from() or a root element to start the batch.");
        }
        final Function<BatchChain<?, ?, ?>, Executable<?, ?>> wrapper = configuration != null && configuration.elementExecutionWrapper != null ?
                e -> configuration.elementExecutionWrapper.apply(e) : e -> e;
        final Runnable execution = () -> {
            Result<?> result = null; // starting node generates a result without a previous one normally
            final var promises = new CopyOnWriteArrayList<CompletableFuture<?>>();
            try {
                for (final var it : chain) {
                    result = wrapper.apply(BatchChain.class.cast(it)).execute(configuration, Result.class.cast(result));
                    if (result.value() instanceof BatchPromise<?> promise) {
                        final var end = promise.end().toCompletableFuture();
                        promises.add(end);
                        end.whenComplete((ok, ko) -> promises.remove(end));
                    }
                }
            } finally {
                await(configuration, promises);
            }
        };
        if (configuration != null && configuration.executionWrapper != null) {
            configuration.executionWrapper.apply(execution).run();
        } else {
            execution.run();
        }
    }

    default BatchChain<P, R, R> filter(final String name, final Predicate<R> filter) {
        final var chain = new BatchChain<P, R, R>() {
            @Override
            public Result<R> execute(final RunConfiguration configuration, final Result<R> previous) {
                return switch (previous.type()) {
                    case SKIP -> new Result<R>(previous.value(), Result.Type.SKIP);
                    case CONTINUE -> {
                        if (filter.test(previous.value())) {
                            yield previous;
                        }
                        yield new Result<>(previous.value(), Result.Type.SKIP);
                    }
                };
            }

            @Override
            public Optional<BatchChain<?, P, R>> previous() {
                return Optional.of(BatchChain.this);
            }

            @Override
            public String name() {
                return name;
            }
        };
        if (Commentifiable.class.isInstance(filter)) {
            return new CommentifiableBatchChain<>(chain, Commentifiable.class.cast(filter), ofNullable(this));
        }
        return chain;
    }

    default <C> BatchChain<P, R, C> map(final String name, final Function<R, C> fn) {
        final var chain = new BatchChain<P, R, C>() {
            @Override
            public Result<C> execute(final RunConfiguration configuration, final Result<R> previous) {
                return switch (previous.type()) {
                    case SKIP -> new Result<>(null, Result.Type.SKIP);
                    case CONTINUE -> new Result<>(fn.apply(previous.value()), Result.Type.CONTINUE);
                };
            }

            @Override
            public Optional<BatchChain<?, P, R>> previous() {
                return Optional.of(BatchChain.this);
            }

            @Override
            public String name() {
                return name;
            }
        };
        if (Commentifiable.class.isInstance(fn)) {
            return new CommentifiableBatchChain<>(chain, Commentifiable.class.cast(fn), ofNullable(this));
        }
        return chain;
    }

    default BatchChain<P, R, R> then(final String name, final Consumer<R> consumer) {
        final var chain = new BatchChain<P, R, R>() {
            @Override
            public Result<R> execute(final RunConfiguration configuration, final Result<R> previous) {
                return switch (previous.type()) {
                    case SKIP -> new Result<>(previous.value(), Result.Type.SKIP);
                    case CONTINUE -> {
                        consumer.accept(previous.value());
                        yield previous;
                    }
                };
            }

            @Override
            public Optional<BatchChain<?, P, R>> previous() {
                return Optional.of(BatchChain.this);
            }

            @Override
            public String name() {
                return name;
            }
        };
        if (Commentifiable.class.isInstance(consumer)) {
            return new CommentifiableBatchChain<>(chain, Commentifiable.class.cast(consumer), ofNullable(this));
        }
        return chain;
    }

    /**
     * Mark a value (of a {@link Result}) as "commentifiable" which means it can be used as representation of the result.
     * Note that it is often stored in a database so keep it short.
     */
    interface Commentifiable {
        default String toComment() {
            return toString();
        }
    }

    record CommentifiableBatchChain<A, B, C>(BatchChain<A, B, C> chain, Commentifiable commentifiable,
                                             Optional<BatchChain<?, A, B>> previous) implements
            BatchChain<A, B, C>, Commentifiable {
        @Override
        public Optional<BatchChain<?, A, B>> previous() {
            return previous;
        }

        @Override
        public String name() {
            return chain.name();
        }

        @Override
        public boolean skipTracing() {
            return chain.skipTracing();
        }

        @Override
        public Result<C> execute(final RunConfiguration configuration, final Result<B> previous) {
            return chain.execute(configuration, previous);
        }

        @Override
        public String toComment() {
            return commentifiable.toComment();
        }
    }

    private void await(final RunConfiguration configuration, final List<CompletableFuture<?>> promises) {
        if (configuration != null && configuration.maxBatchPromiseAwait == 0) {
            return;
        }
        final var duration = configuration == null ? TimeUnit.MINUTES.toMillis(1) : configuration.maxBatchPromiseAwait;
        for (final var stage : new ArrayList<>(promises)) {
            if (stage.isDone() || stage.isCompletedExceptionally()) {
                continue;
            }
            try {
                if (duration < 0) {
                    stage.get();
                } else {
                    stage.get(duration, MILLISECONDS);
                }
            } catch (final InterruptedException ie) {
                Thread.currentThread().interrupt();
            } catch (final ExecutionException | TimeoutException e) {
                Logger.getLogger(getClass().getName()).log(SEVERE, e, e::getMessage);
                if (configuration != null && configuration.failOnTimeout) {
                    promises.forEach(it -> {
                        try {
                            it.cancel(true);
                        } catch (final RuntimeException | Error ex) {
                            // no-op
                        }
                    });
                    throw new IllegalStateException(e);
                }
            }
        }
    }
}
