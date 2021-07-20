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
package io.yupiik.batch.ui.backend;

import io.yupiik.batch.ui.backend.model.Job;
import io.yupiik.batch.ui.backend.model.Page;
import io.yupiik.batch.ui.backend.test.BackendSupport;
import io.yupiik.uship.webserver.tomcat.TomcatWebServerConfiguration;
import jakarta.inject.Inject;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import jakarta.json.bind.Jsonb;
import org.apache.johnzon.mapper.reflection.JohnzonParameterizedType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import javax.sql.DataSource;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;
import java.util.function.Supplier;

import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

@BackendSupport
@TestInstance(PER_CLASS)
class JobExecutionsTest {
    private final HttpClient client = HttpClient.newHttpClient();

    @Inject
    private TomcatWebServerConfiguration configuration;

    @Inject
    private Jsonb jsonb;

    @Inject
    private JsonBuilderFactory factory;

    @Inject
    private DataSource dataSource;

    @Test
    void findJobs() throws IOException, InterruptedException, SQLException {
        final var jobs = fetchJobs(0, 10);
        assertEquals(0, jobs.total());
        assertEquals(List.of(), jobs.items());

        // insert some data to get it - will be deleted with DatabaseSetup callback
        addJobs(10, i -> "test " + i);

        final var jobsPage1 = fetchJobs(0, 5);
        assertEquals(10, jobsPage1.total());
        assertEquals("[" +
                "Job[id=9, name=test 9, status=FAILURE, comment=comment 9, started=2021-06-10T11:49Z, finished=2021-06-10T11:58:01Z, steps=null], " +
                "Job[id=8, name=test 8, status=SUCCESS, comment=comment 8, started=2021-06-09T11:49Z, finished=2021-06-09T11:57:01Z, steps=null], " +
                "Job[id=7, name=test 7, status=FAILURE, comment=comment 7, started=2021-06-08T11:49Z, finished=2021-06-08T11:56:01Z, steps=null], " +
                "Job[id=6, name=test 6, status=SUCCESS, comment=comment 6, started=2021-06-07T11:49Z, finished=2021-06-07T11:55:01Z, steps=null], " +
                "Job[id=5, name=test 5, status=FAILURE, comment=comment 5, started=2021-06-06T11:49Z, finished=2021-06-06T11:54:01Z, steps=null]]", jobsPage1.items().toString());

        final var jobsPage2 = fetchJobs(1, 5);
        assertEquals(10, jobsPage2.total());
        assertEquals("[" +
                "Job[id=4, name=test 4, status=SUCCESS, comment=comment 4, started=2021-06-05T11:49Z, finished=2021-06-05T11:53:01Z, steps=null], " +
                "Job[id=3, name=test 3, status=FAILURE, comment=comment 3, started=2021-06-04T11:49Z, finished=2021-06-04T11:52:01Z, steps=null], " +
                "Job[id=2, name=test 2, status=SUCCESS, comment=comment 2, started=2021-06-03T11:49Z, finished=2021-06-03T11:51:01Z, steps=null], " +
                "Job[id=1, name=test 1, status=FAILURE, comment=comment 1, started=2021-06-02T11:49Z, finished=2021-06-02T11:50:01Z, steps=null], " +
                "Job[id=0, name=test 0, status=SUCCESS, comment=comment 0, started=2021-06-01T11:49Z, finished=2021-06-01T11:49:01Z, steps=null]]", jobsPage2.items().toString());

        final var jobsPage3 = fetchJobs(2, 5);
        assertEquals(10, jobsPage3.total());
        assertEquals(List.of(), jobsPage3.items());
    }

    @Test
    void findLastJobs() throws IOException, InterruptedException, SQLException {
        final Supplier<Page<Job>> fetcher = () -> {
            try {
                return toPage(client.send(
                        HttpRequest.newBuilder()
                                .POST(HttpRequest.BodyPublishers.ofString(factory.createObjectBuilder()
                                        .add("jsonrpc", "2.0")
                                        .add("method", "yupiik-batch-last-executions")
                                        .build()
                                        .toString()))
                                .uri(URI.create("http://localhost:" + configuration.getPort() + "/jsonrpc"))
                                .build(),
                        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)));
            } catch (final IOException | InterruptedException e) {
                return fail(e);
            }
        };

        final var jobs = fetcher.get();
        assertEquals(0, jobs.total());
        assertEquals(List.of(), jobs.items());

        // insert some data to get it - will be deleted with DatabaseSetup callback
        addJobs(10, i -> "test " + (i % 3));

        final var withData = fetcher.get();
        assertEquals(3, withData.total());
        assertEquals("[" +
                        "Job[id=9, name=test 0, status=FAILURE, comment=comment 9, started=2021-06-10T11:49Z, finished=2021-06-10T11:58:01Z, steps=null], " +
                        "Job[id=7, name=test 1, status=FAILURE, comment=comment 7, started=2021-06-08T11:49Z, finished=2021-06-08T11:56:01Z, steps=null], " +
                        "Job[id=8, name=test 2, status=SUCCESS, comment=comment 8, started=2021-06-09T11:49Z, finished=2021-06-09T11:57:01Z, steps=null]]",
                withData.items().toString());
    }

    @Test
    void findJob() throws IOException, InterruptedException, SQLException {
        final var jobs = fetchJobs(0, 10);
        assertEquals(0, jobs.total());
        assertEquals(List.of(), jobs.items());

        // insert some data to get it - will be deleted with DatabaseSetup callback
        addJobs(1, i -> "test " + i);
        addSteps("0", 3);

        final var job = fetchJob("0");
        assertEquals("Job[" +
                "id=0, name=test 0, status=SUCCESS, comment=comment 0, started=2021-06-01T11:49Z, finished=2021-06-01T11:49:01Z, " +
                "steps=[" +
                "Step[id=0, name=test 0, status=SUCCESS, comment=comment 0, started=2021-06-01T11:49Z, finished=2021-06-01T11:49:01Z, previousId=null], " +
                "Step[id=1, name=test 1, status=FAILURE, comment=comment 1, started=2021-06-02T11:49Z, finished=2021-06-02T11:50:01Z, previousId=0], " +
                "Step[id=2, name=test 2, status=SUCCESS, comment=comment 2, started=2021-06-03T11:49Z, finished=2021-06-03T11:51:01Z, previousId=1]]]", job.toString());
    }

    private void addSteps(final String jobId, final int count) throws SQLException {
        try (final var connection = dataSource.getConnection();
             final var statement = connection.prepareStatement("" +
                     "INSERT INTO BATCH_STEP_EXECUTION_TRACE " +
                     " (id, name, status, comment, started, finished, previous_id, job_id) VALUES " +
                     " (?, ?, ?, ?, ?, ?, ?, ?)")) {
            final var from = OffsetDateTime.of(2021, 6, 1, 11, 49, 0, 0, ZoneOffset.UTC);
            for (int i = 0; i < count; i++) {
                statement.setString(1, String.valueOf(i));
                statement.setString(2, "test " + i);
                statement.setString(3, i % 2 == 0 ? "SUCCESS" : "FAILURE");
                statement.setString(4, "comment " + i);
                statement.setObject(5, from.plusDays(i));
                statement.setObject(6, from.plusDays(i).plusMinutes(i).plusSeconds(1));
                statement.setString(7, i == 0 ? null : String.valueOf(i - 1));
                statement.setString(8, jobId);
                statement.addBatch();
            }
            statement.executeBatch();
            connection.commit();
        }
    }

    private void addJobs(final int count, final IntFunction<String> nameFactory) throws SQLException {
        try (final var connection = dataSource.getConnection();
             final var statement = connection.prepareStatement("" +
                     "INSERT INTO BATCH_JOB_EXECUTION_TRACE " +
                     " (id, name, status, comment, started, finished) VALUES " +
                     " (?, ?, ?, ?, ?, ?)")) {
            final var from = OffsetDateTime.of(2021, 6, 1, 11, 49, 0, 0, ZoneOffset.UTC);
            for (int i = 0; i < count; i++) {
                statement.setString(1, String.valueOf(i));
                statement.setString(2, nameFactory.apply(i));
                statement.setString(3, i % 2 == 0 ? "SUCCESS" : "FAILURE");
                statement.setString(4, "comment " + i);
                statement.setObject(5, from.plusDays(i));
                statement.setObject(6, from.plusDays(i).plusMinutes(i).plusSeconds(1));
                statement.addBatch();
            }
            statement.executeBatch();
            connection.commit();
        }
    }

    private Page<Job> fetchJobs(final int page, final int pageSize) throws IOException, InterruptedException {
        final var fetch = client.send(
                HttpRequest.newBuilder()
                        .POST(HttpRequest.BodyPublishers.ofString(factory.createObjectBuilder()
                                .add("jsonrpc", "2.0")
                                .add("method", "yupiik-batch-executions")
                                .add("params", factory.createObjectBuilder()
                                        .add("page", page)
                                        .add("pageSize", pageSize))
                                .build()
                                .toString()))
                        .uri(URI.create("http://localhost:" + configuration.getPort() + "/jsonrpc"))
                        .build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        return toPage(fetch);
    }

    private Page<Job> toPage(final HttpResponse<String> fetch) {
        final var json = assertJsonRpcResult(fetch);
        final var result = json.getJsonObject("result").toString();
        final Page<Map<String, Object>> jobPage = jsonb.fromJson(result, new JohnzonParameterizedType(Page.class, JsonObject.class));
        return new Page<>(jobPage.total(), jobPage.items().stream() // some workaround cause Page looses generics and johnzon does not resolves so much indirections yet
                .map(it -> jsonb.fromJson(jsonb.toJson(it), Job.class))
                .collect(toList()));
    }

    private Job fetchJob(final String id) throws IOException, InterruptedException {
        final var fetch = client.send(
                HttpRequest.newBuilder()
                        .POST(HttpRequest.BodyPublishers.ofString(factory.createObjectBuilder()
                                .add("jsonrpc", "2.0")
                                .add("method", "yupiik-batch-execution")
                                .add("params", factory.createObjectBuilder()
                                        .add("id", id))
                                .build()
                                .toString()))
                        .uri(URI.create("http://localhost:" + configuration.getPort() + "/jsonrpc"))
                        .build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        final var json = assertJsonRpcResult(fetch);
        final var result = json.getJsonObject("result").toString();
        return jsonb.fromJson(result, Job.class);
    }

    private JsonObject assertJsonRpcResult(final HttpResponse<String> fetch) {
        assertEquals(200, fetch.statusCode());

        final var json = jsonb.fromJson(fetch.body(), JsonObject.class);
        assertFalse(json.containsKey("error"), () -> json.get("error").toString());
        assertTrue(json.containsKey("result"));
        return json;
    }
}
