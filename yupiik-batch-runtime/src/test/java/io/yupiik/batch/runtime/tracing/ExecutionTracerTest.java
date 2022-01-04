/*
 * Copyright (c) 2021 - Yupiik SAS - https://www.yupiik.com
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
import static org.junit.jupiter.api.Assertions.assertNull;
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
                    id = resultSet.getString("id");

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
