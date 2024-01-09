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
package io.yupiik.batch.runtime.batch;

import io.yupiik.batch.runtime.sql.DataSourceConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.StringJoiner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BinderTest {
    @Test
    void bind() {
        assertEquals(
                "Configuration[" +
                        "dataSource=DataSourceConfiguration[driver='org.h2.Driver', url='jdbc:h2:mem:test', username='null', password='null'], " +
                        "ds=DataSourceConfiguration[driver='org.h2.Driver2', url='jdbc:h2:mem:test2', username='null', password='null'], " +
                        "dryRun=false, acceptedLoss=0.1, commitInterval=25, table='THE_TABLE']",
                new Binder("a", List.of(
                        "--a-driver", "org.h2.Driver",
                        "--a-url", "jdbc:h2:mem:test",
                        "--a-ds-driver", "org.h2.Driver2",
                        "--a-ds-url", "jdbc:h2:mem:test2"
                )).bind(Configuration.class).toString());
    }

    @Test
    void missingParam() {
        assertEquals("Missing parameter --ds-driver",
                assertThrows(IllegalArgumentException.class, () -> new Binder(null, List.of()).bind(Configuration.class)).getMessage());
    }

    public static class Configuration extends DataSourceConfiguration {
        @Param(name = "dry-run", description = "Run the processing but don't apply it.")
        boolean dryRun;

        @Param(name = "accepted-loss", description = "How many data diff (in size) is accepted and still applied automatically.")
        double acceptedLoss = .1;

        @Param(name = "commit-interval", description = "Size of a batch of inserts/updates/deletes.")
        int commitInterval = 25;

        @Param(name = "table", description = "Which table to update.")
        String table = "THE_TABLE";

        @Param(description = "nested database")
        DataSourceConfiguration ds;

        @Override
        public String toString() {
            return new StringJoiner(", ", Configuration.class.getSimpleName() + "[", "]")
                    .add("dataSource=" + super.toString())
                    .add("ds=" + ds.toString())
                    .add("dryRun=" + dryRun)
                    .add("acceptedLoss=" + acceptedLoss)
                    .add("commitInterval=" + commitInterval)
                    .add("table='" + table + "'")
                    .toString();
        }
    }
}
