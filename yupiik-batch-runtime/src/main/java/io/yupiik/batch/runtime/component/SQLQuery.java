package io.yupiik.batch.runtime.component;

import io.yupiik.batch.runtime.sql.SQLFunction;
import io.yupiik.batch.runtime.sql.SQLSupplier;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.Objects;
import java.util.stream.Stream;

public class SQLQuery<T> implements Iterator<T>, AutoCloseable {
    private final String query;
    private final SQLFunction<ResultSet, T> mapper;

    private Connection connection;
    private Statement statement;
    private ResultSet resultSet;

    public SQLQuery(final SQLSupplier<Connection> connectionSupplier, final String query,
                    final SQLFunction<ResultSet, T> mapper) {
        this.query = query;
        this.mapper = mapper;
        try {
            this.connection = connectionSupplier.get();
        } catch (final SQLException throwables) {
            throw new IllegalStateException(throwables);
        }
    }

    @Override
    public boolean hasNext() {
        if (statement == null) {
            try {
                this.statement = connection.createStatement();
                this.resultSet = statement.executeQuery(query);
            } catch (final SQLException throwables) {
                throw new IllegalStateException(throwables);
            }
        }
        try {
            return resultSet.next();
        } catch (final SQLException throwables) {
            throw new IllegalStateException(throwables);
        }
    }

    @Override
    public T next() {
        try {
            return mapper.apply(resultSet);
        } catch (final SQLException throwables) {
            throw new IllegalStateException(throwables);
        }
    }

    @Override
    public void close() {
        final var error = new IllegalStateException("An error occured closing " + getClass());
        Stream.of(resultSet, statement, connection)
                .filter(Objects::nonNull)
                .forEach(it -> {
                    try {
                        it.close();
                    } catch (final Exception e) {
                        error.addSuppressed(e);
                    }
                });
        connection = null;
        statement = null;
        resultSet = null;
        if (error.getSuppressed().length > 0) {
            throw error;
        }
    }
}
