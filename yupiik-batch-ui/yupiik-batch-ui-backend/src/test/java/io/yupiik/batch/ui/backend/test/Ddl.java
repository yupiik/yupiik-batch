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
package io.yupiik.batch.ui.backend.test;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;

import javax.sql.DataSource;
import java.sql.SQLException;

@Dependent
public class Ddl {
    public void onStart(@Observes @Initialized(ApplicationScoped.class) final Object start,
                        final DataSource dataSource) {
        try (final var connection = dataSource.getConnection();
             final var statement = connection.createStatement()) {
            statement.execute("" +
                    "CREATE TABLE BATCH_JOB_EXECUTION_TRACE (" +
                    "   id VARCHAR(64)," +
                    "   name VARCHAR(128)," +
                    "   status VARCHAR(16)," +
                    "   comment CLOB," +
                    "   started TIMESTAMP," +
                    "   finished TIMESTAMP," +
                    "   PRIMARY KEY (id)" +
                    ")");
            statement.execute("" +
                    "CREATE TABLE BATCH_STEP_EXECUTION_TRACE (" +
                    "   id VARCHAR(64)," +
                    "   job_id VARCHAR(64)," +
                    "   previous_id VARCHAR(64)," +
                    "   name VARCHAR(128)," +
                    "   status VARCHAR(16)," +
                    "   comment CLOB," +
                    "   started TIMESTAMP," +
                    "   finished TIMESTAMP," +
                    "   PRIMARY KEY (id)" +
                    ")");
            connection.commit();
        } catch (final SQLException throwables) {
            throw new IllegalStateException(throwables);
        }
    }
}
