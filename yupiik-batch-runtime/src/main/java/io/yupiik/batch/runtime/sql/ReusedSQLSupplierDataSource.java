/*
 * Copyright (c) 2021-2022 - Yupiik SAS - https://www.yupiik.com
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
package io.yupiik.batch.runtime.sql;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Wrapper;
import java.util.logging.Logger;

// easy bridge between datasource configuration and DataSource API
public class ReusedSQLSupplierDataSource implements DataSource, AutoCloseable {
    private final SQLSupplier<Connection> connectionSQLSupplier;

    private Connection connection;

    public ReusedSQLSupplierDataSource(final SQLSupplier<Connection> connectionSQLSupplier) {
        this.connectionSQLSupplier = connectionSQLSupplier;
    }

    @Override
    public synchronized Connection getConnection() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            return connection;
        }
        final var delegate = connectionSQLSupplier.get();
        if (!delegate.getAutoCommit()) { // all the logic relies on that since we don't handle explicitly commits but only bulks
            delegate.setAutoCommit(true);
        }
        connection = Connection.class.cast(
                Proxy.newProxyInstance(
                        Thread.currentThread().getContextClassLoader(),
                        new Class<?>[]{Connection.class, Wrapper.class},
                        (proxy, method, args) -> {
                            if ("close".equals(method.getName())) {
                                return null;
                            }
                            if ("unwrap".equals(method.getName())) {
                                return delegate;
                            }
                            try {
                                return method.invoke(delegate, args);
                            } catch (final InvocationTargetException ite) {
                                throw ite.getTargetException();
                            }
                        }
                )
        );
        return connection;
    }

    @Override
    public Connection getConnection(final String username, final String password) throws SQLException {
        return getConnection();
    }

    @Override
    public PrintWriter getLogWriter() {
        return DriverManager.getLogWriter();
    }

    @Override
    public void setLogWriter(final PrintWriter out) {
        // no-op
    }

    @Override
    public void setLoginTimeout(final int seconds) {
        // no-op
    }

    @Override
    public int getLoginTimeout() {
        return 0;
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public <T> T unwrap(final Class<T> iface) {
        return iface.isInstance(this) ? iface.cast(this) : null;
    }

    @Override
    public boolean isWrapperFor(final Class<?> iface) {
        return iface.isInstance(this);
    }

    @Override
    public synchronized void close() throws Exception {
        if (connection != null) {
            Wrapper.class.cast(connection).unwrap(Connection.class).close();
            connection = null;
        }
    }
}
