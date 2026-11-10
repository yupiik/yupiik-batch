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
import io.yupiik.fusion.http.server.impl.tomcat.TomcatWebServerConfiguration;
import io.yupiik.fusion.testing.Fusion;
import io.yupiik.fusion.testing.FusionSupport;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static java.net.http.HttpResponse.BodyHandlers.ofString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@FusionSupport
class MonitoringTest {
    private final HttpClient client = HttpClient.newHttpClient();

    @Test
    void monitoringServerEnabledByDefault(@Fusion final WebServer.Configuration configuration) throws IOException, InterruptedException {
        final var monitoring = configuration.unwrap(TomcatWebServerConfiguration.class).getMonitoringServerConfiguration();
        assertNotNull(monitoring, "monitoring server must be enabled by default");

        assertEquals(200, get(monitoring.getPort(), "/health").statusCode());
        assertEquals(200, get(monitoring.getPort(), "/metrics").statusCode());

        // not exposed on the main port
        assertEquals(404, get(configuration.port(), "/health").statusCode());
    }

    private HttpResponse<String> get(final int port, final String path) throws IOException, InterruptedException {
        return client.send(HttpRequest.newBuilder().GET().uri(URI.create("http://localhost:" + port + path)).build(), ofString());
    }
}
