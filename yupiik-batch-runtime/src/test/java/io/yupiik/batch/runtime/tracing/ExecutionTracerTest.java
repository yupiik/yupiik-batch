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

import io.yupiik.batch.runtime.batch.Batch;
import io.yupiik.batch.runtime.batch.BatchMeta;
import io.yupiik.batch.runtime.batch.BatchPromise;
import io.yupiik.batch.runtime.batch.builder.BatchChain;
import io.yupiik.batch.runtime.batch.builder.Executable;
import io.yupiik.batch.runtime.batch.builder.RunConfiguration;
import io.yupiik.batch.runtime.fn.CommentifiableConsumer;
import io.yupiik.batch.runtime.fn.CommentifiableFunction;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static java.time.Clock.systemUTC;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class ExecutionTracerTest {
    @Test
    void asyncSteps() {
        final var exec = new AtomicReference<BaseExecutionTracer.JobExecution>();
        final var allSteps = new AtomicReference<List<BaseExecutionTracer.StepExecution>>();
        final var tracer = new BaseExecutionTracer("test", Clock.systemUTC()) {

            @Override
            protected void save(final JobExecution execution, final List<StepExecution> steps) {
                assertTrue(exec.compareAndSet(null, execution));
                assertTrue(allSteps.compareAndSet(null, steps));
            }
        };

        final var conf = new RunConfiguration();
        conf.setExecutionWrapper(tracer::traceExecution);
        conf.setElementExecutionWrapper(e -> (c, r) -> Executable.Result.class.cast(tracer.traceStep(c, e, r)));

        new AsyncStepsBatch().accept(conf);

        assertNotNull(exec.get());
        final var steps = allSteps.get();
        assertEquals(List.of("step1", "step2"), steps.stream().map(BaseExecutionTracer.StepExecution::name).collect(toList()));
        final var d1 = Duration.between(steps.get(1).started(), steps.get(0).finished()).toMillis();
        assertTrue(d1 >= 10, () -> steps + ": " + d1);
        final var d2 = Duration.between(steps.get(0).finished(), steps.get(1).finished()).toMillis();
        assertTrue(d2 >= 10, () -> steps + ": " + d2);
    }

    @Test
    void trace() throws SQLException {
        final var dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:SQLQueryTest_normal");
        try (final var keepDb = dataSource.getConnection()) { // avoid h2 to delete the table with the last close()
            try (final var connection = dataSource.getConnection();
                 final var statement = connection.createStatement()) {
                statement.execute("" +
                        "CREATE TABLE BATCH_JOB_EXECUTION_TRACE (" +
                        "   id VARCHAR(64)," +
                        "   name VARCHAR(128)," +
                        "   status VARCHAR(16)," +
                        "   comment CLOB," +
                        "   started TIMESTAMP," +
                        "   finished TIMESTAMP," +
                        "   PRIMARY KEY (id)" +
                        ")");
                statement.execute("" +
                        "CREATE TABLE BATCH_STEP_EXECUTION_TRACE (" +
                        "   id VARCHAR(64)," +
                        "   job_id VARCHAR(64)," +
                        "   name VARCHAR(128)," +
                        "   status VARCHAR(16)," +
                        "   comment CLOB," +
                        "   started TIMESTAMP," +
                        "   finished TIMESTAMP," +
                        "   previous_id VARCHAR(64)," +
                        "   PRIMARY KEY (id)" +
                        ")");
                connection.commit();
            }

            final var tracer = new ExecutionTracer(dataSource::getConnection, "test", systemUTC());
            assertThrows(IllegalStateException.class, () -> tracer.traceExecution(() -> {
                tracer.traceStep(null, new BatchChain() {
                    @Override
                    public Result execute(final RunConfiguration configuration, final Result previous) {
                        return null;
                    }

                    @Override
                    public Optional<BatchChain> previous() {
                        return Optional.empty();
                    }

                    @Override
                    public String name() {
                        return "step1";
                    }
                }, null);
                tracer.traceStep(null, new BatchChain() {
                    @Override
                    public Result execute(final RunConfiguration configuration, final Result previous) {
                        return null;
                    }

                    @Override
                    public Optional<BatchChain> previous() {
                        return Optional.empty();
                    }

                    @Override
                    public String name() {
                        return "step2";
                    }
                }, null);
                tracer.traceStep(null, new BatchChain() {
                    @Override
                    public Result execute(final RunConfiguration configuration, final Result previous) {
                        throw new IllegalStateException("error for test");
                    }

                    @Override
                    public Optional<BatchChain> previous() {
                        return Optional.empty();
                    }

                    @Override
                    public String name() {
                        return "step3";
                    }
                }, null);
            }).run());
            assertTrue(tracer.isAlreadySaved());

            try (final var connection = dataSource.getConnection();
                 final var statement = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
                try (final var resultSet = statement.executeQuery(
                        "SELECT id, name, comment, status, previous_id from BATCH_STEP_EXECUTION_TRACE ORDER BY name")) {
                    String id;
                    assertTrue(resultSet.next());
                    assertEquals("step1", resultSet.getString("name"));
                    assertEquals("SUCCESS", resultSet.getString("status"));
                    assertNull(resultSet.getString("previous_id"));
                    id = resultSet.getString("id");

                    assertTrue(resultSet.next());
                    assertEquals("step2", resultSet.getString("name"));
                    assertEquals("SUCCESS", resultSet.getString("status"));
                    assertEquals(id, resultSet.getString("previous_id"));
                    id = resultSet.getString("id");

                    assertTrue(resultSet.next());
                    assertEquals("step3", resultSet.getString("name"));
                    assertEquals("error for test", resultSet.getString("comment"));
                    assertEquals("FAILURE", resultSet.getString("status"));
                    assertEquals(id, resultSet.getString("previous_id"));
                    resultSet.getString("id");

                    assertFalse(resultSet.next(), () -> {
                        try {
                            return resultSet.getString("name");
                        } catch (final SQLException throwables) {
                            return fail(throwables);
                        }
                    });
                }
                try (final var resultSet = statement.executeQuery("SELECT name, status, comment from BATCH_JOB_EXECUTION_TRACE ORDER BY name")) {
                    assertTrue(resultSet.next());
                    assertEquals("test", resultSet.getString("name"));
                    assertEquals("FAILURE", resultSet.getString("status"));
                    assertEquals("error for test", resultSet.getString("comment"));

                    assertFalse(resultSet.next(), () -> {
                        try {
                            return resultSet.getString("name");
                        } catch (final SQLException throwables) {
                            return fail(throwables);
                        }
                    });
                }
            }
        }
    }

    @BatchMeta(description = "")
    public static class AsyncStepsBatch implements Batch<RunConfiguration /* for test, not a great real case */> {
        @Override
        public void accept(final RunConfiguration conf) {
            from()
                    .map("step1", new CommentifiableFunction<Void, BatchPromise<String>>() {
                        @Override
                        public BatchPromise<String> apply(final Void o) {
                            return BatchPromise.of("from-step-1", new CompletableFuture<>());
                        }

                        @Override
                        public String toComment() {
                            return "step #1";
                        }
                    })
                    .then("step2", new CommentifiableConsumer<>() {
                        @Override
                        public void accept(final BatchPromise<String> in) {
                            assertEquals("from-step-1", in.value());
                            try {
                                Thread.sleep(10); // just to have an instant which moved
                            } catch (final InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                            if (in.end() instanceof CompletableFuture<Void> p) { // complete in step 2 to show it is handled (after step 2 start)
                                p.complete(null);
                            }
                            try {
                                Thread.sleep(10); // just to have an instant which moved
                            } catch (final InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        }

                        @Override
                        public String toComment() {
                            return "step #2";
                        }
                    })
                    .run(conf);
        }
    }
}
