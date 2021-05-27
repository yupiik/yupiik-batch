package io.yupiik.batch.runtime.component;

import io.yupiik.batch.runtime.component.diff.Diff;
import io.yupiik.batch.runtime.model.Simple;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class DiffExecutorTest {
    @Test
    void apply() throws SQLException {
        final var diff = newDiff();
        final var dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:DiffExecutorTest_apply");
        try (final var keepDb = dataSource.getConnection()) { // avoid h2 to delete the table with the last close()
            seed(dataSource, "apply");

            new DiffExecutor<>(
                    dataSource::getConnection, 10, false,
                    () -> new Simple.Insert("DiffExecutorTest_apply"),
                    () -> new Simple.Update("DiffExecutorTest_apply"),
                    () -> new Simple.Delete("DiffExecutorTest_apply"))
                    .accept(diff);

            try (final var connection = dataSource.getConnection();
                 final var statement = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
                 final var resultSet = statement.executeQuery("SELECT name, age from DiffExecutorTest_apply ORDER BY name")) {
                assertTrue(resultSet.next());
                assertEquals("12346", resultSet.getString("name"));
                assertEquals(3, resultSet.getInt("age"));

                assertTrue(resultSet.next());
                assertEquals("12347", resultSet.getString("name"));
                assertEquals(2, resultSet.getInt("age"));

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

    @Test
    void dryRun() throws SQLException {
        final var diff = newDiff();
        final var dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:DiffExecutorTest_dryRun");
        final var logger = Logger.getLogger(DiffExecutor.class.getName());
        final var records = new ArrayList<LogRecord>();
        final var handler = new Handler() {
            @Override
            public synchronized void publish(final LogRecord record) {
                if (record.getLevel() == Level.INFO) {
                    records.add(record);
                }
            }

            @Override
            public void flush() {
                // no-op
            }

            @Override
            public void close() throws SecurityException {
                flush();
            }
        };
        logger.addHandler(handler);
        try (final var keepDb = dataSource.getConnection()) { // avoid h2 to delete the table with the last close()
            seed(dataSource, "dryRun");

            new DiffExecutor<>(
                    dataSource::getConnection, 10, true,
                    () -> new Simple.Insert("DiffExecutorTest_dryRun"),
                    () -> new Simple.Update("DiffExecutorTest_dryRun"),
                    () -> new Simple.Delete("DiffExecutorTest_dryRun"))
                    .accept(diff);

            // ensure database didn't change
            try (final var connection = dataSource.getConnection();
                 final var statement = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
                 final var resultSet = statement.executeQuery("SELECT name, age from DiffExecutorTest_dryRun ORDER BY name")) {
                assertTrue(resultSet.next());
                assertEquals("12345", resultSet.getString("name"));
                assertEquals(1, resultSet.getInt("age"));

                assertTrue(resultSet.next());
                assertEquals("12346", resultSet.getString("name"));
                assertEquals(2, resultSet.getInt("age"));

                assertFalse(resultSet.next(), () -> {
                    try {
                        return resultSet.getString("name");
                    } catch (final SQLException throwables) {
                        return fail(throwables);
                    }
                });
            }
        }
        // ensure all operations were logged even if not applied
        assertEquals(
                """
                Diff summary:
                     To Add: 1
                  To Remove: 1
                  To Update: 1
                [C][S] Starting transaction
                [d][A] Adding Simple[name=12347, age=2]
                [C][E] Finished transaction
                [C][S] Starting transaction
                [d][U] Updating Simple[name=12346, age=3]
                [C][E] Finished transaction
                [C][S] Starting transaction
                [d][D] Deleting Simple[name=12345, age=1]
                [C][E] Finished transaction""",
                records.stream().map(LogRecord::getMessage).collect(joining("\n")));
    }

    private Diff<Simple> newDiff() {
        return new Diff<>(
                List.of(new Simple("12345", 1)),
                List.of(new Simple("12347", 2)),
                List.of(new Simple("12346", 3)),
                -1);
    }

    private void seed(final DataSource dataSource, final String mtd) throws SQLException {
        try (final var connection = dataSource.getConnection();
             final var statement = connection.createStatement()) {
            statement.execute("" +
                    "CREATE TABLE DiffExecutorTest_" + mtd + " (" +
                    "   name VARCHAR(50) NOT NULL," +
                    "   age INT," +
                    "   PRIMARY KEY (name)" +
                    ")");
            statement.execute("" +
                    "INSERT INTO DiffExecutorTest_" + mtd +
                    " (name, age) VALUES " +
                    " ('12345', 1)");
            statement.execute("" +
                    "INSERT INTO DiffExecutorTest_" + mtd +
                    " (name, age) VALUES " +
                    " ('12346', 2)");
            connection.commit();
        }
    }
}
