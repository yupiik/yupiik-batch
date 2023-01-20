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

import io.yupiik.batch.runtime.component.diff.Diff;
import io.yupiik.batch.runtime.fn.CommentifiableConsumer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class BaseDiffExecutor<T> implements CommentifiableConsumer<Diff<T>> {
    protected final Logger logger = Logger.getLogger(getClass().getName());
    protected final StringBuilder comments = new StringBuilder();
    protected final boolean dryRun;
    protected final int commitInterval;

    public BaseDiffExecutor(final boolean dryRun, final int commitInterval) {
        this.dryRun = dryRun;
        this.commitInterval = commitInterval;
    }

    protected abstract void batchInsert(Class<T> type, Iterator<T> iterator);

    protected abstract void batchUpdate(Class<T> type, Iterator<T> iterator);

    protected abstract void batchDelete(Class<T> type, Iterator<T> iterator);

    protected String logMarker() {
        return "";
    }

    @Override
    public void accept(final Diff<T> diff) {
        logger.info(() -> "" +
                "Diff summary" + logMarker() + ":\n" +
                "     To Add: " + diff.added().size() + "\n" +
                "  To Remove: " + diff.deleted().size() + "\n" +
                "  To Update: " + diff.updated().size());
        final var prefix = dryRun ? "[d]" : "";
        if (!diff.added().isEmpty()) {
            withCommitInterval(
                    diff.added().iterator(), prefix + "[A] Adding ",
                    dryRun ? this::noop : this::batchInsert);
        } else {
            logger.info(() -> "No insert" + logMarker());
        }
        if (!diff.updated().isEmpty()) {
            withCommitInterval(
                    diff.updated().iterator(), prefix + "[U] Updating ",
                    dryRun ? this::noop : this::batchUpdate);
        } else {
            logger.info(() -> "No update" + logMarker());
        }
        if (!diff.deleted().isEmpty()) {
            withCommitInterval(
                    diff.deleted().iterator(), prefix + "[D] Deleting ",
                    dryRun ? this::noop : this::batchDelete);
        } else {
            logger.info(() -> "No deletion" + logMarker());
        }

        comments.append(diff.toComment()).append('\n');
    }

    private <A> void withCommitInterval(final Iterator<A> rows,
                                        final String logPrefix,
                                        final BiConsumer<Class<A>, Iterator<A>> handler) {
        final var entities = new ArrayList<A>();
        try {
            while (rows.hasNext()) {
                logger.info("[C][S] Starting transaction");
                try {
                    for (int i = 0; i < commitInterval && rows.hasNext(); i++) {
                        final var row = rows.next();
                        logger.info(() -> logPrefix + row);
                        entities.add(row);
                    }
                    if (entities.isEmpty()) {
                        return;
                    }
                    final var entityType = entities.get(0).getClass();
                    handler.accept((Class<A>) entityType, entities.iterator());
                    entities.clear();
                } catch (final RuntimeException ex) {
                    onException(ex);
                    throw ex;
                } catch (final Exception ex) {
                    onException(ex);
                    throw new IllegalStateException(ex);
                } finally {
                    logger.info("[C][E] Finished transaction");
                }
            }
        } catch (final RuntimeException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private <A> void noop(final Class<A> type, final Iterator<A> list) {
        // no-op
    }

    private void onException(final Exception ex) {
        comments.append(ex.getMessage()).append('\n');
        logger.log(Level.SEVERE, ex.getMessage(), ex);
    }

    @Override
    public String toComment() {
        return comments.toString().strip();
    }
}
