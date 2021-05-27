package io.yupiik.batch.runtime.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@FunctionalInterface
public interface SQLBiConsumer<A, B> {
    void accept(A param1, B param2) throws SQLException;

    static <A, B> SQLBiConsumer<A, B> noop() {
        return (param1, param2) -> {
        };
    }

    abstract class Batched<R> implements SQLBiConsumer<Connection, R>, AutoCloseable {
        protected PreparedStatement statement;

        protected abstract PreparedStatement createStatement(Connection connection) throws SQLException;

        protected abstract void doAccept(R row) throws SQLException;

        @Override
        public final void accept(final Connection connection, final R row) throws SQLException {
            if (statement == null) {
                statement = createStatement(connection);
            }
            doAccept(row);
            statement.addBatch();
        }

        @Override
        public void close() throws SQLException {
            if (statement != null) {
                statement.executeBatch();
            }
        }
    }
}