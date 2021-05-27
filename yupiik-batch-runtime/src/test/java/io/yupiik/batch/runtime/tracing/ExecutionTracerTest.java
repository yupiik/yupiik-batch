package io.yupiik.batch.runtime.tracing;

import io.yupiik.batch.runtime.batch.builder.BatchChain;
import io.yupiik.batch.runtime.batch.builder.RunConfiguration;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import static java.time.Clock.systemUTC;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class ExecutionTracerTest {
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
                        "   PRIMARY KEY (id)" +
                        ")");
                connection.commit();
            }

            assertThrows(IllegalStateException.class, () -> {
                final var tracer = new ExecutionTracer(dataSource::getConnection, "test", systemUTC());
                tracer.traceExecution(() -> {
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
                }).run();
            });

            try (final var connection = dataSource.getConnection();
                 final var statement = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
                try (final var resultSet = statement.executeQuery("SELECT name, comment, status from BATCH_STEP_EXECUTION_TRACE ORDER BY name")) {
                    assertTrue(resultSet.next());
                    assertEquals("step1", resultSet.getString("name"));
                    assertEquals("SUCCESS", resultSet.getString("status"));

                    assertTrue(resultSet.next());
                    assertEquals("step2", resultSet.getString("name"));
                    assertEquals("SUCCESS", resultSet.getString("status"));

                    assertTrue(resultSet.next());
                    assertEquals("step3", resultSet.getString("name"));
                    assertEquals("error for test", resultSet.getString("comment"));
                    assertEquals("FAILURE", resultSet.getString("status"));

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
}
