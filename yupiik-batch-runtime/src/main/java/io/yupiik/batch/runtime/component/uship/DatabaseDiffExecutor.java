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
package io.yupiik.batch.runtime.component.uship;

import io.yupiik.batch.runtime.component.BaseDiffExecutor;
import io.yupiik.uship.persistence.api.Database;

import java.util.Iterator;
import java.util.logging.Logger;

public class DatabaseDiffExecutor<T> extends BaseDiffExecutor<T> {
    private final Logger logger = Logger.getLogger(getClass().getName());
    private final StringBuilder comments = new StringBuilder();
    private final Database database;
    private final String table;

    public DatabaseDiffExecutor(final Database database, final boolean dryRun,
                                final int commitInterval, final String table) {
        super(dryRun, commitInterval);
        this.database = database;
        this.table = table;
    }

    @Override
    protected String logMarker() {
        return " for '" + table + "'";
    }

    @Override
    protected void batchInsert(final Class<T> type, final Iterator<T> iterator) {
        database.batchInsert(type, iterator);
    }

    @Override
    protected void batchUpdate(final Class<T> type, final Iterator<T> iterator) {
        database.batchUpdate(type, iterator);
    }

    @Override
    protected void batchDelete(final Class<T> type, final Iterator<T> iterator) {
        database.batchDelete(type, iterator);
    }
}
