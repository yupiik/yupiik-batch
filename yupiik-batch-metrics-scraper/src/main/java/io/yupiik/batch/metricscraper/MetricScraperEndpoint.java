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
package io.yupiik.batch.metricscraper;

import io.yupiik.fusion.framework.build.api.http.HttpMatcher;
import io.yupiik.fusion.http.server.api.Request;
import io.yupiik.fusion.http.server.api.Response;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

import static io.yupiik.fusion.framework.build.api.http.HttpMatcher.PathMatching.STARTS_WITH;
import static java.util.Optional.ofNullable;
import static java.util.logging.Level.SEVERE;
import static java.util.stream.Collectors.joining;

public class MetricScraperEndpoint {

    private final static Logger logger = Logger.getLogger(MetricScraperEndpoint.class.getName());

    private final MetricsRelayStorage storage;

    public MetricScraperEndpoint(MetricsRelayStorage storage) {
        this.storage = storage;
    }

    @HttpMatcher(path = "/relay", methods = "POST", pathMatching = STARTS_WITH)
    public Response storeMetrics(final Request request) {
        final String responseContentType = "text/plain";
        final boolean dropOnPull = Boolean.parseBoolean(ofNullable(request.parameter("dropOnPull")).orElse("false"));
        final String id = ofNullable(request.parameter("id")).orElse("");

        try (final var in = read(request)) {
            storage.store(id, in.lines().collect(joining("\n")).strip(), dropOnPull);
        } catch (final IOException | ExecutionException | InterruptedException exception) {
            Logger.getLogger(getClass().getName()).log(SEVERE, exception, exception::getMessage);
            return Response.of().body("KO").header("Content-Type", responseContentType).build();
        }
        return Response.of().body("OK").status(201).header("Content-Type", responseContentType).build();
    }

    @HttpMatcher(path = "/relay", methods = "GET", pathMatching = STARTS_WITH)
    public Response fetchMetrics(final Request request) {
        final String responseContentType = "application/openmetrics-text; version=1.0.0; charset=utf-8";
        if (storage.getMetrics().isEmpty()) {
            return Response.of().header("Content-Type", responseContentType).build();
        }

        final boolean ignoreDrop = Boolean.parseBoolean(ofNullable(request.parameter("ignoreDrop")).orElse("false"));
        final String payload = Stream.of(storage.getMetrics().toArray(new MetricsRelayStorage.Entry[0]))
                .peek(entry -> {
                    if (!ignoreDrop && entry.dropOnPull()) {
                        storage.remove(entry); // it is a CopyOnWriteArrayList so it.remove() does not work
                    }
                })
                .map(MetricsRelayStorage.Entry::content).sorted().collect(joining("\n", "", "\n# EOF\n"));

        return Response.of().body(payload).status(200).header("Content-Type", responseContentType).build();
    }

    private BufferedReader read(final Request request) throws IOException, ExecutionException, InterruptedException {
        final var encoding = request.header("content-encoding");
        final var byteArrayIs = new ByteArrayInputStream(request.fullBody().bytes().toCompletableFuture().get());
        if (encoding != null && encoding.contains("gzip")) { // our client sends in gzip depending a limit
            return new BufferedReader(new InputStreamReader(new GZIPInputStream(byteArrayIs)));
        }
        return new BufferedReader(new InputStreamReader(byteArrayIs));
    }
}