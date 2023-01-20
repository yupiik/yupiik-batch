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
package io.yupiik.batch.runtime.component;

import io.yupiik.batch.runtime.documentation.Component;
import io.yupiik.batch.runtime.sql.SQLBiConsumer;
import io.yupiik.batch.runtime.sql.SQLSupplier;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component("""
        Enables to apply a `Diff` - from a `DatasetDiffComputer`.
                
        It will apply it in a database represented by the `connectionSupplier` with the provided `commitInterval`.
        The statements are creating using the related factories - `insertFactory`, `updateFactory`, `deleteFactory`.
                
        Finally, `dryRun` toggle enables to simulate the processing without issuing any modification in the database.""")
public class DiffExecutor<A> extends BaseDiffExecutor<A> {
    private final Logger logger = Logger.getLogger(getClass().getName());
    private final SQLSupplier<Connection> connectionSupplier;
    private final Supplier<? extends SQLBiConsumer<Connection, A>> insertFactory;
    private final Supplier<? extends SQLBiConsumer<Connection, A>> updateFactory;
    private final Supplier<? extends SQLBiConsumer<Connection, A>> deleteFactory;

    public DiffExecutor(final SQLSupplier<Connection> connectionSupplier,
                        final int commitInterval, final boolean dryRun,
                        final Supplier<? extends SQLBiConsumer<Connection, A>> insertFactory,
                        final Supplier<? extends SQLBiConsumer<Connection, A>> updateFactory,
                        final Supplier<? extends SQLBiConsumer<Connection, A>> deleteFactory) {
        super(dryRun, commitInterval);
        this.connectionSupplier = connectionSupplier;
        this.insertFactory = insertFactory;
        this.updateFactory = updateFactory;
        this.deleteFactory = deleteFactory;
    }

    @Override
    protected void batchInsert(final Class<A> type, final Iterator<A> iterator) {
        handle(iterator, newInsert());
    }

    @Override
    protected void batchUpdate(Class<A> type, final Iterator<A> iterator) {
        handle(iterator, newUpdate());
    }

    @Override
    protected void batchDelete(final Class<A> type, final Iterator<A> iterator) {
        handle(iterator, newDelete());
    }

    protected SQLBiConsumer<Connection, A> newDelete() {
        return deleteFactory.get();
    }

    protected SQLBiConsumer<Connection, A> newUpdate() {
        return updateFactory.get();
    }

    protected SQLBiConsumer<Connection, A> newInsert() {
        return insertFactory.get();
    }

    private void handle(final Iterator<A> rows,
                        final SQLBiConsumer<Connection, A> onRow) {
        if (!rows.hasNext()) {
            return;
        }
        try (final var connection = connectionSupplier.get()) { // todo: retry if connection fails
            while (rows.hasNext()) {
                final boolean autoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    while (rows.hasNext()) {
                        onRow.accept(connection, rows.next());
                    }
                    if (AutoCloseable.class.isInstance(onRow)) {
                        AutoCloseable.class.cast(onRow).close();
                    }
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
            }
        } catch (final RuntimeException | SQLException sqle) {
            throw new IllegalStateException(sqle);
        }
    }

    private void onException(final Connection connection, final Exception ex) throws SQLException {
        connection.rollback();
        logger.log(Level.SEVERE, ex.getMessage(), ex);
    }

    public static <A> DiffExecutor<A> applyDiff(final SQLSupplier<Connection> connectionSupplier,
                                                final int commitInterval, final boolean dryRun,
                                                final Supplier<SQLBiConsumer<Connection, A>> insertFactory,
                                                final Supplier<SQLBiConsumer<Connection, A>> updateFactory,
                                                final Supplier<SQLBiConsumer<Connection, A>> deleteFactory) {
        return new DiffExecutor<>(connectionSupplier, commitInterval, dryRun, insertFactory, updateFactory, deleteFactory);
    }
}
