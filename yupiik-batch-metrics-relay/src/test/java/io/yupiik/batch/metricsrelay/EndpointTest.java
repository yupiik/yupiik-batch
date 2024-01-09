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
package io.yupiik.batch.metricsrelay;

import io.yupiik.fusion.http.server.api.WebServer;
import io.yupiik.fusion.testing.Fusion;
import io.yupiik.fusion.testing.FusionSupport;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

import static java.net.http.HttpResponse.BodyHandlers.ofString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@FusionSupport
public class EndpointTest {

    private final static Logger logger = Logger.getLogger(EndpointTest.class.getName());

    private final HttpClient client = HttpClient.newHttpClient();

    private HttpResponse<String> sendPostRequest(final WebServer.Configuration configuration, final String contextPath, final String payload) throws IOException, InterruptedException {
        return client.send(
                HttpRequest.newBuilder()
                        .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                        .uri(URI.create("http://localhost:" + configuration.port() + contextPath)).build(),
                ofString());
    }

    private HttpResponse<String> sendGetRequest(final WebServer.Configuration configuration, final String contextPath) throws IOException, InterruptedException {
        return client.send(
                HttpRequest.newBuilder()
                        .GET()
                        .uri(URI.create("http://localhost:" + configuration.port() + contextPath)).build(),
                ofString());
    }

    @Test
    void checkMetrics(@Fusion final WebServer.Configuration configuration) throws IOException, InterruptedException {
        final var storeMetricResponse = sendPostRequest(
                configuration,
                "/relay?id=test&dropOnPull=true",
                """
                # TYPE foo1 gauge
                # HELP foo1 doc
                foo1 1234
                # TYPE foo2 gauge
                # HELP foo2 doc
                foo2 1235
                """);
        assertEquals(201, storeMetricResponse.statusCode());
        assertEquals("OK", storeMetricResponse.body());

        final var fetchMetricResponse = sendGetRequest(
                configuration,
                "/relay?id=test&ignoreDrop=true");
        assertEquals(200, fetchMetricResponse.statusCode());
        assertEquals("""
                # TYPE foo1 gauge
                # HELP foo1 doc
                foo1 1234
                # TYPE foo2 gauge
                # HELP foo2 doc
                foo2 1235
                # EOF
                """, fetchMetricResponse.body());

        final var fetchMetricResponse2 = sendGetRequest(
                configuration,
                "/relay?id=test");
        assertEquals(200, fetchMetricResponse2.statusCode());
        assertEquals("""
                # TYPE foo1 gauge
                # HELP foo1 doc
                foo1 1234
                # TYPE foo2 gauge
                # HELP foo2 doc
                foo2 1235
                # EOF
                """, fetchMetricResponse2.body());

        final var fetchMetricResponseEmpty = sendGetRequest(
                configuration,
                "/relay?id=test");
        assertEquals(200, fetchMetricResponseEmpty.statusCode());
        assertEquals("""
                # EOF
                """, fetchMetricResponseEmpty.body());
    }
}
