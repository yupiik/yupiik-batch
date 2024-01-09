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
package io.yupiik.batch.ui.backend;

import io.yupiik.batch.runtime.util.Substitutor;
import io.yupiik.batch.ui.backend.configuration.Configuration;
import io.yupiik.batch.ui.backend.model.Job;
import io.yupiik.batch.ui.backend.model.Page;
import io.yupiik.batch.ui.backend.model.Status;
import io.yupiik.batch.ui.backend.model.Step;
import io.yupiik.batch.ui.backend.sql.IteratingResultset;
import io.yupiik.uship.jsonrpc.core.api.JsonRpc;
import io.yupiik.uship.jsonrpc.core.api.JsonRpcMethod;
import io.yupiik.uship.jsonrpc.core.api.JsonRpcParam;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Comparator.comparing;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

@JsonRpc
@ApplicationScoped
public class JobExecutions {
    @Inject
    private DataSource dataSource;

    @Inject
    private Configuration configuration;

    private String countAllJobs;
    private String findLastExecutions;

    @PostConstruct
    private void init() {
        final var simpleQueryInterpolator = new Substitutor(key -> switch (key) {
            case "table" -> configuration.getJobTable();
            default -> throw new IllegalStateException("Unknown key '" + key + "'");
        });
        countAllJobs = simpleQueryInterpolator.replace(configuration.getCountAllJobs());
        findLastExecutions = simpleQueryInterpolator.replace(configuration.getFindLastExecutions());
    }

    @JsonRpcMethod(name = "yupiik-batch-executions", documentation = "Returns the paginated executions, not that steps are not populated.")
    public Page<Job> findJobs(@JsonRpcParam(required = true, documentation = "Page to fetch.") final int page,
                              @JsonRpcParam(documentation = "Filter a single batch executions.") final String batch,
                              @JsonRpcParam(required = true, documentation = "Size of the page (max being 100).") final int pageSize) throws SQLException {
        final int actualPageSize = Math.max(0, Math.min(50, pageSize));
        final var filterByName = batch != null && !batch.isBlank();
        final var countSql = countAllJobs + (filterByName ? " WHERE name = ?" : "");
        final var bindSelect = new AtomicBoolean();
        final var selectSql = new Substitutor(key -> switch (key) {
            case "table" -> configuration.getJobTable();
            case "pageSize" -> String.valueOf(actualPageSize);
            case "firstIndex" -> String.valueOf(actualPageSize * page);
            case "lastIndex" -> String.valueOf(actualPageSize * (1 + page));
            case "where" -> {
                bindSelect.set(filterByName);
                yield filterByName ? "WHERE name = ? " : "";
            }
            default -> throw new IllegalStateException("Unknown key '" + key + "'");
        }).replace(configuration.getFindAllJobs());

        try (final var connection = dataSource.getConnection();
             final var countStmt = connection.prepareStatement(countSql);
             final var itemStmt = connection.prepareStatement(selectSql)) {

            if (filterByName) {
                countStmt.setString(1, batch);
                if (bindSelect.get()) {
                    itemStmt.setString(1, batch);
                }
            }

            try (final var countResult = countStmt.executeQuery();
                 final var itemResultSet = itemStmt.executeQuery()) {
                final long total = countResult.next() ? countResult.getLong(1) : 0;
                final var items = IteratingResultset.toList(itemResultSet, r -> new Job(
                        r.getString(1),
                        r.getString(2),
                        ofNullable(r.getString(3)).map(Status::valueOf).orElse(null),
                        r.getString(4),
                        r.getObject(5, OffsetDateTime.class).withOffsetSameInstant(ZoneOffset.UTC),
                        r.getObject(6, OffsetDateTime.class).withOffsetSameInstant(ZoneOffset.UTC),
                        null));
                return new Page<>(total, items);
            }
        }
    }

    @JsonRpcMethod(name = "yupiik-batch-last-executions", documentation = "Returns the last execution of each batch to be able to build an overview page.")
    public Page<Job> findLastJobs() throws SQLException {
        try (final var connection = dataSource.getConnection();
             final var stmt = connection.createStatement();
             final var result = stmt.executeQuery(findLastExecutions)) {
            final var items = IteratingResultset.toList(result, r -> new Job(
                    r.getString(1),
                    r.getString(2),
                    ofNullable(r.getString(3)).map(Status::valueOf).orElse(null),
                    r.getString(4),
                    r.getObject(5, OffsetDateTime.class).withOffsetSameInstant(ZoneOffset.UTC),
                    r.getObject(6, OffsetDateTime.class).withOffsetSameInstant(ZoneOffset.UTC),
                    null));
            return new Page<>(items.size(), items);
        }
    }

    @JsonRpcMethod(name = "yupiik-batch-execution", documentation = "Returns the related job with its steps populated.")
    public Job findJobById(@JsonRpcParam(required = true, documentation = "Job to fetch.") final String id) throws SQLException {
        try (final var connection = dataSource.getConnection();
             final var jobResultSetStmt = connection.prepareStatement(new Substitutor(key -> switch (key) {
                 case "table" -> configuration.getJobTable();
                 default -> throw new IllegalStateException("Unknown key '" + key + "'");
             }).replace(configuration.getFindJobById()))) {
            jobResultSetStmt.setString(1, id);
            try (final var jobResultSet = jobResultSetStmt.executeQuery()) {
                if (!jobResultSet.next()) {
                    throw new IllegalArgumentException("No job #" + id + " found.");
                }
                try (final var stepStmt = connection.prepareStatement(new Substitutor(key -> switch (key) {
                    case "table" -> configuration.getStepTable();
                    default -> throw new IllegalStateException("Unknown key '" + key + "'");
                }).replace(configuration.getFindStepsByJobId()))) {
                    stepStmt.setString(1, id);
                    try (final var stepResultSet = stepStmt.executeQuery()) {
                        final var steps = IteratingResultset.toList(stepResultSet, r -> new Step(
                                r.getString(1),
                                r.getString(2),
                                ofNullable(r.getString(3)).map(Status::valueOf).orElse(null),
                                r.getString(4),
                                r.getObject(5, OffsetDateTime.class).withOffsetSameInstant(ZoneOffset.UTC),
                                r.getObject(6, OffsetDateTime.class).withOffsetSameInstant(ZoneOffset.UTC),
                                r.getString(7)));
                        return new Job(
                                jobResultSet.getString(1),
                                jobResultSet.getString(2),
                                ofNullable(jobResultSet.getString(3)).map(Status::valueOf).orElse(null),
                                jobResultSet.getString(4),
                                jobResultSet.getObject(5, OffsetDateTime.class).withOffsetSameInstant(ZoneOffset.UTC),
                                jobResultSet.getObject(6, OffsetDateTime.class).withOffsetSameInstant(ZoneOffset.UTC),
                                steps.stream().sorted(comparing(Step::started)).collect(toList()));
                    }
                }
            }
        }
    }
}
