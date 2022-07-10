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
package io.yupiik.batch.ui.backend.test;

import jakarta.enterprise.inject.spi.CDI;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import javax.sql.DataSource;

public class DatabaseSetup implements AfterEachCallback {
    static {
        System.setProperty("yupiik.batch.backend.tomcat.port", "0"); // random
        System.setProperty("yupiik.batch.backend.datasource.driver", "org.h2.Driver");
        System.setProperty("yupiik.batch.backend.datasource.url", "jdbc:h2:mem:yupiik-batch;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
    }

    @Override
    public void afterEach(final ExtensionContext context) throws Exception {
        final var datasource = CDI.current().select(DataSource.class).get();
        try (final var connection = datasource.getConnection();
             final var truncateJobs = connection.createStatement();
             final var truncateSteps = connection.createStatement()) {
            truncateJobs.execute("TRUNCATE TABLE BATCH_JOB_EXECUTION_TRACE");
            truncateSteps.execute("TRUNCATE TABLE BATCH_STEP_EXECUTION_TRACE");
            connection.commit();
        }
    }
}
