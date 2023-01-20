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
package io.yupiik.batch.runtime.model;

import io.yupiik.batch.runtime.sql.SQLBiConsumer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.Objects;
import java.util.function.BiPredicate;

public record Simple(String name, int age) {
    public static class KeyComparator implements Comparator<Simple> {
        @Override
        public int compare(final Simple o1, final Simple o2) {
            return o1.name.compareTo(o2.name);
        }
    }

    public static class ValueTester implements BiPredicate<Simple, Simple> {
        @Override
        public boolean test(final Simple o1, final Simple o2) {
            return Objects.equals(o1, o2);
        }
    }

    public static class Insert extends SQLBiConsumer.Batched<Simple> {
        protected final String table;

        public Insert(final String table) {
            this.table = table;
        }

        @Override
        protected PreparedStatement createStatement(final Connection connection) throws SQLException {
            return connection.prepareStatement("INSERT INTO " + table + " (name, age) VALUES (?, ?)");
        }

        @Override
        protected void doAccept(final Simple row) throws SQLException {
            statement.setString(1, row.name());
            statement.setInt(2, row.age());
        }
    }

    public static class Update extends SQLBiConsumer.Batched<Simple> {
        protected final String table;

        public Update(final String table) {
            this.table = table;
        }

        @Override
        protected PreparedStatement createStatement(final Connection connection) throws SQLException {
            return connection.prepareStatement("UPDATE " + table + " SET age = ? WHERE name = ?");
        }

        @Override
        protected void doAccept(final Simple row) throws SQLException {
            statement.setInt(1, row.age());
            statement.setString(2, row.name());
        }
    }

    public static class Delete extends SQLBiConsumer.Batched<Simple> {
        protected final String table;

        public Delete(final String table) {
            this.table = table;
        }

        @Override
        protected PreparedStatement createStatement(final Connection connection) throws SQLException {
            return connection.prepareStatement("DELETE FROM " + table + " WHERE name = ?");
        }

        @Override
        protected void doAccept(final Simple row) throws SQLException {
            statement.setString(1, row.name());
        }
    }
}
