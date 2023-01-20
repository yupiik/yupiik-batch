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