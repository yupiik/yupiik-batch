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
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

/**
 * Ensures the bundled frontend is served by Tomcat (router for SPA routes and default servlet for static assets).
 */
@BackendSupport
@TestInstance(PER_CLASS)
class FrontendTest {
    private static final Pattern MAIN_JS = Pattern.compile("src=\"(/static/js/main\\.[^\"]+\\.js)\"");
    private static final Pattern MAIN_CSS = Pattern.compile("href=\"(/static/css/main\\.[^\"]+\\.css)\"");

    private final HttpClient client = HttpClient.newHttpClient();

    @Inject
    private TomcatWebServerConfiguration configuration;

    @Test
    void indexAndSpaRoutes() throws IOException, InterruptedException {
        final var index = get("/");
        assertEquals(200, index.statusCode());
        assertTrue(index.headers().firstValue("content-type").orElse("").startsWith("text/html"), index.headers()::toString);
        assertTrue(index.body().contains("<div id=\"root\"></div>"), index::body);

        for (final var route : new String[]{"/index.html", "/home", "/executions", "/last-executions", "/execution/123", "/extensions/foo"}) {
            final var response = get(route);
            assertEquals(200, response.statusCode(), route);
            assertEquals(index.body(), response.body(), route);
        }
    }

    @Test
    void staticAssets() throws IOException, InterruptedException {
        final var index = get("/").body();
        for (final var pattern : new Pattern[]{MAIN_JS, MAIN_CSS}) {
            final var matcher = pattern.matcher(index);
            assertTrue(matcher.find(), () -> "no " + pattern + " in " + index);
            final var asset = get(matcher.group(1));
            assertEquals(200, asset.statusCode(), matcher.group(1));
            assertFalse(asset.body().isBlank(), matcher.group(1));
        }

        for (final var path : new String[]{"/favicon.ico", "/robots.txt", "/asset-manifest.json"}) {
            assertEquals(200, get(path).statusCode(), path);
        }

        assertEquals(404, get("/static/js/missing.js").statusCode());
    }

    private HttpResponse<String> get(final String path) throws IOException, InterruptedException {
        return client.send(
                HttpRequest.newBuilder().GET().uri(URI.create("http://localhost:" + configuration.getPort() + path)).build(),
                HttpResponse.BodyHandlers.ofString(UTF_8));
    }
}
