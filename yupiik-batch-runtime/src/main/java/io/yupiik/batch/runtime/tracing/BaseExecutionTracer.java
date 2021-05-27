package io.yupiik.batch.runtime.tracing;

import io.yupiik.batch.runtime.batch.builder.BatchChain;
import io.yupiik.batch.runtime.batch.builder.Executable;
import io.yupiik.batch.runtime.batch.builder.RunConfiguration;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class BaseExecutionTracer {
    private final Clock clock;
    private final String batchName;

    private final List<StepExecution> steps = new CopyOnWriteArrayList<>();

    public BaseExecutionTracer(final String batchName, final Clock clock) {
        this.clock = clock;
        this.batchName = batchName;
    }

    protected abstract void save(final JobExecution execution, final List<StepExecution> steps);

    public Executable.Result<?> traceStep(final RunConfiguration configuration,
                                          final BatchChain<?, ?, ?> batchChain, final BatchChain.Result<?> previous) {
        final var start = clock.instant();
        var status = Status.SUCCESS;
        String comment = null;
        try {
            final var executed = batchChain.execute(configuration, Executable.Result.class.cast(previous));
            if (executed != null && executed.value() != null && BatchChain.Commentifiable.class.isInstance(executed.value())) {
                comment = BatchChain.Commentifiable.class.cast(executed.value()).toComment();
            }
            return executed;
        } catch (final Error | RuntimeException err) {
            status = Status.FAILURE;
            comment = err.getMessage();
            throw err;
        } finally {
            if (!batchChain.skipTracing()) {
                final var end = clock.instant();
                final var execution = new StepExecution(
                        UUID.randomUUID().toString(), batchChain.name(), status, comment,
                        LocalDateTime.ofInstant(start, clock.getZone()), LocalDateTime.ofInstant(end, clock.getZone()));
                steps.add(execution);
            }
        }
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

    public enum Status {
        SUCCESS, FAILURE
    }

    public static record StepExecution(
            String id, String name, Status status, String comment,
            LocalDateTime started, LocalDateTime finished) {
    }

    public static record JobExecution(
            String id, String name, Status status, String comment,
            LocalDateTime started, LocalDateTime finished) {
    }
}
