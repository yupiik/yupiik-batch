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
package io.yupiik.batch.runtime.tracing;

import io.yupiik.batch.runtime.batch.BatchPromise;
import io.yupiik.batch.runtime.batch.builder.BatchChain;
import io.yupiik.batch.runtime.batch.builder.Executable;
import io.yupiik.batch.runtime.batch.builder.RunConfiguration;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

import static java.util.logging.Level.SEVERE;

public abstract class BaseExecutionTracer {
    private final Clock clock;
    private final String batchName;
    protected final boolean forceSkip;

    private final List<StepExecution> steps = new CopyOnWriteArrayList<>();

    public BaseExecutionTracer(final String batchName, final Clock clock) {
        this(batchName, clock, false);
    }

    public BaseExecutionTracer(final String batchName, final Clock clock, final boolean forceSkip) {
        this.clock = clock;
        this.batchName = batchName;
        this.forceSkip = forceSkip;
    }

    protected abstract void save(final JobExecution execution, final List<StepExecution> steps);

    public Executable.Result<?> traceStep(final RunConfiguration configuration,
                                          final BatchChain<?, ?, ?> batchChain, final BatchChain.Result<?> previous) {
        if (forceSkip || batchChain.skipTracing()) {
            return batchChain.execute(configuration, Executable.Result.class.cast(previous));
        }

        final var start = clock.instant();
        var status = Status.SUCCESS;
        String comment = null;
        boolean async = false;
        try {
            final var executed = batchChain.execute(configuration, Executable.Result.class.cast(previous));
            final var hasValue = executed != null && executed.value() != null;
            final var chainIsCommentiafiable = BatchChain.Commentifiable.class.isInstance(batchChain);
            if (hasValue && chainIsCommentiafiable && !(executed.value() instanceof BatchPromise<?>)) {
                comment = BatchChain.Commentifiable.class.cast(batchChain).toComment();
            }
            if (hasValue) {
                if (executed.value() instanceof BatchPromise<?> promise) {
                    async = true;
                    promise.end().whenComplete((ok, ko) -> {
                        String asyncComment = null;
                        if (chainIsCommentiafiable) {
                            asyncComment = BatchChain.Commentifiable.class.cast(batchChain).toComment();
                        }

                        if (executed.value() instanceof BatchChain.Commentifiable c) {
                            asyncComment = (asyncComment == null || asyncComment.isBlank() ? "" : (asyncComment + '\n')) + c.toComment();
                        }
                        if (ko != null) {
                            endStep(batchChain, start, Status.FAILURE, getErrorMessage(asyncComment, ko));
                        } else {
                            endStep(batchChain, start, Status.SUCCESS, asyncComment);
                        }
                    });
                } else if (executed.value() instanceof BatchChain.Commentifiable c) {
                    comment = (comment == null || comment.isBlank() ? "" : (comment + '\n')) + c.toComment();
                }
            }
            return executed;
        } catch (final Error | RuntimeException err) {
            status = Status.FAILURE;
            comment = getErrorMessage(comment, err);
            async = false;
            throw err;
        } finally {
            if (!async) {
                endStep(batchChain, start, status, comment);
            }
        }
    }

    protected String getErrorMessage(final String comment, final Throwable err) {
        return (comment != null ? comment + '\n' : "") + err.getMessage();
    }

    protected void endStep(final BatchChain<?, ?, ?> batchChain,
                           final Instant start, final Status status,
                           final String comment) {
        final var end = clock.instant();
        final var execution = new StepExecution(
                UUID.randomUUID().toString(), batchChain.name(), status, comment,
                LocalDateTime.ofInstant(start, clock.getZone()), LocalDateTime.ofInstant(end, clock.getZone()),
                steps.isEmpty() ? null : steps.get(steps.size() - 1).id());
        steps.add(execution);
    }

    public Runnable traceExecution(final Runnable runnable) {
        return () -> {
            final var start = clock.instant();
            var status = Status.SUCCESS;
            String error = null;
            try {
                runnable.run();
            } catch (final Error | RuntimeException err) {
                status = Status.FAILURE;
                error = err.getMessage();
                throw err;
            } finally {
                final var end = clock.instant();
                final var execution = new JobExecution(
                        UUID.randomUUID().toString(), batchName, status, error,
                        LocalDateTime.ofInstant(start, clock.getZone()), LocalDateTime.ofInstant(end, clock.getZone()));
                save(execution, steps);
            }
        };
    }

    public static RunConfiguration trace(final RunConfiguration configuration, final BaseExecutionTracer tracer) {
        configuration.setExecutionWrapper(tracer::traceExecution);
        configuration.setElementExecutionWrapper(e -> (c, r) -> {
            try {
                return Executable.Result.class.cast(tracer.traceStep(c, e, r));
            } catch (final RuntimeException re) {
                Logger.getLogger(ExecutionTracer.class.getName()).log(SEVERE, re, re::getMessage);
                throw re;
            }
        });
        return configuration;
    }

    public enum Status {
        SUCCESS, FAILURE
    }

    public record StepExecution(
            String id, String name, Status status, String comment,
            LocalDateTime started, LocalDateTime finished, String previous) {
    }

    public record JobExecution(
            String id, String name, Status status, String comment,
            LocalDateTime started, LocalDateTime finished) {
    }
}
