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
package io.yupiik.batch.runtime.component;

import io.yupiik.batch.runtime.component.diff.Diff;
import io.yupiik.batch.runtime.sql.SQLBiConsumer;
import io.yupiik.batch.runtime.sql.SQLSupplier;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import static io.yupiik.batch.runtime.sql.SQLBiConsumer.noop;

public class DiffExecutor<A> implements Consumer<Diff<A>> {
    private final Logger logger = Logger.getLogger(getClass().getName());
    private final SQLSupplier<Connection> connectionSupplier;
    private final int commitInterval;
    private final boolean dryRun;
    private final Supplier<? extends SQLBiConsumer<Connection, A>> insertFactory;
    private final Supplier<? extends SQLBiConsumer<Connection, A>> updateFactory;
    private final Supplier<? extends SQLBiConsumer<Connection, A>> deleteFactory;

    public DiffExecutor(final SQLSupplier<Connection> connectionSupplier,
                        final int commitInterval, final boolean dryRun,
                        final Supplier<? extends SQLBiConsumer<Connection, A>> insertFactory,
                        final Supplier<? extends SQLBiConsumer<Connection, A>> updateFactory,
                        final Supplier<? extends SQLBiConsumer<Connection, A>> deleteFactory) {
        this.connectionSupplier = connectionSupplier;
        this.commitInterval = commitInterval;
        this.dryRun = dryRun;
        this.insertFactory = insertFactory;
        this.updateFactory = updateFactory;
        this.deleteFactory = deleteFactory;
    }

    @Override
    public void accept(final Diff<A> referenceRowDiff) {
        logger.info(() -> "" +
                "Diff summary:\n" +
                "     To Add: " + referenceRowDiff.added().size() + "\n" +
                "  To Remove: " + referenceRowDiff.deleted().size() + "\n" +
                "  To Update: " + referenceRowDiff.updated().size());
        final var prefix = dryRun ? "[d]" : "";
        withCommitInterval(
                referenceRowDiff.added().iterator(), prefix + "[A] Adding ",
                dryRun ? noop() : newInsert());
        withCommitInterval(
                referenceRowDiff.updated().iterator(), prefix + "[U] Updating ",
                dryRun ? noop() : newUpdate());
        withCommitInterval(
                referenceRowDiff.deleted().iterator(), prefix + "[D] Deleting ",
                dryRun ? noop() : newDelete());
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

    private void withCommitInterval(final Iterator<A> rows,
                                    final String logPrefix,
                                    final SQLBiConsumer<Connection, A> onRow) {
        try (final var connection = connectionSupplier.get()) { // todo: retry if connection fails
            while (rows.hasNext()) {
                final boolean autoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                logger.info("[C][S] Starting transaction");
                try {
                    for (int i = 0; i < commitInterval && rows.hasNext(); i++) {
                        final var row = rows.next();
                        logger.info(() -> logPrefix + row);
                        if (onRow != null) {
                            onRow.accept(connection, row);
                        }
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
                    logger.info("[C][E] Finished transaction");
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
