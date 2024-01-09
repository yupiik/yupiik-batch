/*
 * Copyright (c) 2021-present - Yupiik SAS - https://www.yupiik.com
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
package io.yupiik.batch.runtime.component.uship;

import io.yupiik.batch.runtime.component.diff.Diff;
import io.yupiik.batch.runtime.model.Simple;
import io.yupiik.uship.persistence.api.Column;
import io.yupiik.uship.persistence.api.Database;
import io.yupiik.uship.persistence.api.Id;
import io.yupiik.uship.persistence.api.Table;
import io.yupiik.uship.persistence.api.bootstrap.Configuration;
import io.yupiik.uship.persistence.impl.datasource.SimpleDataSource;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.atomic.LongAdder;

import static io.yupiik.uship.persistence.api.StatementBinder.NONE;
import static java.util.Comparator.comparing;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseDiffExecutorTest {
    @Test
    void apply() throws SQLException {
        final var diff = newDiff();
        final var dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:DatabaseDiffExecutorTest_apply");
        try (final var keepDb = dataSource.getConnection()) { // avoid h2 to delete the table with the last close()
            seed(dataSource, "apply");

            final var counter = new LongAdder();
            final var database = Database.of(new Configuration()
                    .setDataSource(new SimpleDataSource(null, null, null) {
                        @Override
                        public Connection getConnection() throws SQLException {
                            return Connection.class.cast(Proxy.newProxyInstance(
                                    Thread.currentThread().getContextClassLoader(), new Class<?>[]{Connection.class},
                                    new InvocationHandler() {
                                        private final Connection connection;

                                        {
                                            connection = dataSource.getConnection();
                                            counter.increment();
                                        }

                                        @Override
                                        public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
                                            try {
                                                return method.invoke(connection, args);
                                            } catch (final InvocationTargetException ite) {
                                                throw ite.getTargetException();
                                            } finally {
                                                if ("close".equals(method.getName())) {
                                                    counter.decrement();
                                                }
                                            }
                                        }
                                    }));
                        }
                    }));
            new DatabaseDiffExecutor<MyEntity>(database, false, 10, "DatabaseDiffExecutorTest_apply")
                    .accept(diff);
            assertEquals(0, counter.sum());

            final var data = database.query(MyEntity.class, database.getOrCreateEntity(MyEntity.class).getFindAllQuery(), NONE)
                    .stream()
                    .sorted(comparing(e -> e.name))
                    .iterator();
            assertTrue(data.hasNext());
            final var e1 = data.next();
            assertEquals("12346", e1.name);
            assertEquals(3, e1.age);

            assertTrue(data.hasNext());
            final var e2 = data.next();
            assertEquals("12347", e2.name);
            assertEquals(2, e2.age);

            assertFalse(data.hasNext());
        }
    }

    private Diff<MyEntity> newDiff() {
        return new Diff<>(
                List.of(new MyEntity("12345", 1)),
                List.of(new MyEntity("12347", 2)),
                List.of(new MyEntity("12346", 3)),
                -1, -1);
    }

    private void seed(final DataSource dataSource, final String mtd) throws SQLException {
        try (final var connection = dataSource.getConnection();
             final var statement = connection.createStatement()) {
            statement.execute("" +
                    "CREATE TABLE DatabaseDiffExecutorTest_" + mtd + " (" +
                    "   name VARCHAR(50) NOT NULL," +
                    "   age INT," +
                    "   PRIMARY KEY (name)" +
                    ")");
            statement.execute("" +
                    "INSERT INTO DatabaseDiffExecutorTest_" + mtd +
                    " (name, age) VALUES " +
                    " ('12345', 1)");
            statement.execute("" +
                    "INSERT INTO DatabaseDiffExecutorTest_" + mtd +
                    " (name, age) VALUES " +
                    " ('12346', 2)");
            connection.commit();
        }
    }

    @Table("DatabaseDiffExecutorTest_apply")
    public static class MyEntity {
        @Id
        private String name;

        @Column
        private int age;

        public MyEntity() {
            // no-op
        }

        public MyEntity(final String name, final int age) {
            this.name = name;
            this.age = age;
        }
    }
}
