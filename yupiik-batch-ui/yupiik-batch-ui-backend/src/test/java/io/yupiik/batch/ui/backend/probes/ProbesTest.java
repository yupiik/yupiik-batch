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
package io.yupiik.batch.ui.backend.probes;

import io.yupiik.batch.ui.backend.test.BackendSupport;
import io.yupiik.uship.webserver.tomcat.TomcatWebServerConfiguration;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

@BackendSupport
@TestInstance(PER_CLASS)
class ProbesTest {
    private final HttpClient client = HttpClient.newHttpClient();

    @Inject
    private TomcatWebServerConfiguration configuration;

    @Test
    void live() throws IOException, InterruptedException {
        assertEquals(200, get("/api/health/live"));
    }

    @Test
    void ready() throws IOException, InterruptedException {
        assertEquals(200, get("/api/health/ready"));
    }

    private int get(final String path) throws IOException, InterruptedException {
        return client.send(
                        HttpRequest.newBuilder().GET().uri(URI.create("http://localhost:" + configuration.getPort() + path)).build(),
                        HttpResponse.BodyHandlers.discarding())
                .statusCode();
    }
}
