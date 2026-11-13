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
import jakarta.json.Json;
import jakarta.json.JsonString;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

/**
 * Ensures the bundled frontend is served by Tomcat (router for SPA routes and default servlet for static assets).
 */
@BackendSupport
@TestInstance(PER_CLASS)
class FrontendTest {
    private static final Pattern SCRIPT = Pattern.compile("<script\\b[^>]*\\bsrc=[\"']([^\"']+)[\"'][^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern DEFER = Pattern.compile("\\sdefer(?:\\s|=|>)", Pattern.CASE_INSENSITIVE);
    private static final Pattern LINK = Pattern.compile("<link\\b[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern STYLESHEET = Pattern.compile("\\brel=[\"']stylesheet[\"']", Pattern.CASE_INSENSITIVE);
    private static final Pattern HREF = Pattern.compile("\\bhref=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);

    private final HttpClient client = HttpClient.newHttpClient();

    @Inject
    private TomcatWebServerConfiguration configuration;
    @Test
    void bundledTemplatePlacesMarkerBeforeAllDeferredScripts() throws IOException {
        try (final var in = getClass().getClassLoader().getResourceAsStream("META-INF/resources/index.html")) {
            assertNotNull(in, "Missing bundled index.html");
            final var html = new String(in.readAllBytes(), UTF_8);
            final var marker = "<!--yupiik-batch-extensions-->";
            final int position = html.indexOf(marker);
            assertTrue(position >= 0, () -> "Missing " + marker + " in " + html);
            assertEquals(position, html.lastIndexOf(marker), "Duplicate extensions marker");
            final var script = SCRIPT.matcher(html);
            int deferred = 0;
            while (script.find()) {
                if (DEFER.matcher(script.group()).find()) {
                    assertTrue(position < script.start(), () -> "Extensions marker must precede " + script.group());
                    assertFalse(script.group().contains("type=\"module\""), script::group);
                    deferred++;
                }
            }
            assertTrue(deferred > 0, () -> "No deferred scripts in " + html);
        }
    }


    @Test
    void indexAndSpaRoutes() throws IOException, InterruptedException {
        final var index = get("/");
        assertEquals(200, index.statusCode());
        assertTrue(index.headers().firstValue("content-type").orElse("").startsWith("text/html"), index.headers()::toString);
        assertTrue(index.body().contains("<div id=\"root\"></div>"), index::body);
        assertFalse(index.body().contains("<!--yupiik-batch-extensions-->"), index::body);

        for (final var route : new String[]{"/index.html", "/home", "/executions", "/last-executions", "/execution/123", "/extensions/foo"}) {
            final var response = get(route);
            assertEquals(200, response.statusCode(), route);
            assertEquals(index.body(), response.body(), route);
        }
    }

    @Test
    void staticAssets() throws IOException, InterruptedException {
        final var index = get("/").body();
        final var scripts = new LinkedHashSet<String>();
        final var script = SCRIPT.matcher(index);
        while (script.find()) {
            scripts.add(script.group(1));
        }
        assertFalse(scripts.isEmpty(), () -> "No scripts in " + index);
        final var styles = new LinkedHashSet<String>();
        final var link = LINK.matcher(index);
        while (link.find()) {
            if (STYLESHEET.matcher(link.group()).find()) {
                final var href = HREF.matcher(link.group());
                assertTrue(href.find(), link::group);
                styles.add(href.group(1));
            }
        }
        assertFalse(styles.isEmpty(), () -> "No stylesheets in " + index);
        final var assets = new LinkedHashSet<>(scripts);
        assets.addAll(styles);
        assertAssets(assets);

        for (final var path : new String[]{"/favicon.ico", "/robots.txt"}) {
            assertEquals(200, get(path).statusCode(), path);
        }

        final var manifest = get("/asset-manifest.json");
        assertEquals(200, manifest.statusCode());
        try (final var reader = Json.createReader(new StringReader(manifest.body()))) {
            final var json = reader.readObject();
            final var files = json.getJsonObject("files");
            assertNotNull(files, manifest::body);
            assertFalse(files.isEmpty(), manifest::body);
            final var manifestAssets = new LinkedHashSet<String>();
            for (final var value : files.values()) {
                manifestAssets.add(assetPath(((JsonString) value).getString()));
            }
            assertTrue(manifestAssets.containsAll(assets), () -> "HTML assets missing from manifest: " + assets + " in " + json);
            assertAssets(manifestAssets);
            final var entrypoints = json.getJsonArray("entrypoints");
            assertNotNull(entrypoints, manifest::body);
            assertFalse(entrypoints.isEmpty(), manifest::body);
            for (final var entrypoint : entrypoints.getValuesAs(JsonString.class)) {
                final var path = assetPath(entrypoint.getString());
                assertTrue(assets.contains(path), () -> "Entrypoint missing from HTML: " + path);
            }
        }

        for (final var path : new String[]{"/static/js/missing.js", "/static/css/missing.css", "/static/missing.png"}) {
            assertEquals(404, get(path).statusCode(), path);
        }
    }

    private void assertAssets(final Set<String> paths) throws IOException, InterruptedException {
        for (final var path : paths) {
            final var asset = get(assetPath(path));
            assertEquals(200, asset.statusCode(), path);
            assertFalse(asset.body().isBlank(), path);
            if (!path.endsWith(".html")) {
                assertFalse(asset.headers().firstValue("content-type").orElse("").startsWith("text/html"), path);
            }
        }
    }

    private String assetPath(final String path) {
        return path.startsWith("/") ? path : "/" + path;
    }

    private HttpResponse<String> get(final String path) throws IOException, InterruptedException {
        return client.send(
                HttpRequest.newBuilder().GET().uri(URI.create("http://localhost:" + configuration.getPort() + path)).build(),
                HttpResponse.BodyHandlers.ofString(UTF_8));
    }
}
