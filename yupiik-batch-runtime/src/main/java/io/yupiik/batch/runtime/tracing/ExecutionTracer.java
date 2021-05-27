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

import io.yupiik.batch.runtime.batch.builder.Executable;
import io.yupiik.batch.runtime.batch.builder.RunConfiguration;
import io.yupiik.batch.runtime.sql.SQLBiConsumer;
import io.yupiik.batch.runtime.sql.SQLSupplier;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Clock;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ExecutionTracer extends BaseExecutionTracer {
    private final SQLSupplier<Connection> dataSource;

    public ExecutionTracer(final SQLSupplier<Connection> dataSource,
                           final String batchName, final Clock clock) {
        super(batchName, clock);
        this.dataSource = dataSource;
    }

    @Override
    protected void save(final JobExecution job, final List<StepExecution> steps) {
        try (final var connection = dataSource.get();
             final var statement = connection.prepareStatement("" +
                     "INSERT INTO BATCH_JOB_EXECUTION_TRACE" +
                     " (id, name, status, comment, started, finished) VALUES " +
                     " (?, ?, ?, ?, ?, ?)")) {
            statement.setString(1, job.id());
            statement.setString(2, job.name());
            statement.setString(3, job.status() == null ? "-" : job.status().name());
            statement.setString(4, job.comment());
            statement.setObject(5, job.started());
            statement.setObject(6, job.finished());
            statement.executeUpdate();
            if (!connection.getAutoCommit()) {
                connection.commit();
            }
        } catch (final SQLException throwables) {
            throw new IllegalStateException(throwables);
        }

        final var insert = new SQLBiConsumer.Batched<StepExecution>() {
            @Override
            protected PreparedStatement createStatement(final Connection connection) throws SQLException {
                return connection.prepareStatement("" +
                        "INSERT INTO BATCH_STEP_EXECUTION_TRACE" +
                        " (id, job_id, name, status, comment, started, finished) VALUES" +
                        " (?, ?, ?, ?, ?, ?, ?)");
            }

            @Override
            protected void doAccept(final StepExecution row) throws SQLException {
                statement.setString(1, row.id());
                statement.setString(2, job.id());
                statement.setString(3, row.name());
                statement.setString(4, row.status() == null ? "-" : row.status().name());
                statement.setString(5, row.comment());
                statement.setObject(6, row.started());
                statement.setObject(7, row.finished());
            }
        };
        final int commitInterval = 10; // likely sufficient in one iteration since #steps < 10 in general
        final var stepsIt = steps.iterator();
        while (stepsIt.hasNext()) {
            try {
                final var connection = dataSource.get();
                final boolean autoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    for (int i = 0; i < commitInterval && stepsIt.hasNext(); i++) {
                        final var row = stepsIt.next();
                        insert.accept(connection, row);
                    }
                    insert.close();
                    connection.commit();
                } catch (final RuntimeException | SQLException ex) {
                    onException(connection, ex);
                    throw ex;
                } catch (final Exception ex) {
                    onException(connection, ex);
                    throw new IllegalStateException(ex);
                } finally {
                    connection.setAutoCommit(autoCommit);
                }
            } catch (final RuntimeException | SQLException sqle) {
                throw new IllegalStateException(sqle);
            }
        }
    }

    private void onException(final Connection connection, final Exception ex) throws SQLException {
        connection.rollback();
        Logger.getLogger(getClass().getName()).log(Level.SEVERE, ex.getMessage(), ex);
    }

    public RunConfiguration chilRunConfiguration() {
        final var configuration = new RunConfiguration();
        configuration.setElementExecutionWrapper(e -> (c, r) -> Executable.Result.class.cast(traceStep(c, e, r)));
        return configuration;
    }

    public static RunConfiguration runConfiguration(final SQLSupplier<Connection> dataSource, final String batch, final Clock clock) {
        return trace(new RunConfiguration(), dataSource, batch, clock);
    }

    public static RunConfiguration trace(final RunConfiguration configuration,
                                         final SQLSupplier<Connection> dataSource, final String batch, final Clock clock) {
        final var tracer = new ExecutionTracer(dataSource, batch, clock);
        configuration.setExecutionWrapper(tracer::traceExecution);
        configuration.setElementExecutionWrapper(e -> (c, r) -> Executable.Result.class.cast(tracer.traceStep(c, e, r)));
        return configuration;
    }
}
