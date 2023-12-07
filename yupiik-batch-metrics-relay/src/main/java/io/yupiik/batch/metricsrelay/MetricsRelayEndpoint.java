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
package io.yupiik.batch.metricsrelay;

import io.yupiik.fusion.framework.api.scope.ApplicationScoped;
import io.yupiik.fusion.framework.build.api.http.HttpMatcher;
import io.yupiik.fusion.http.server.api.Request;
import io.yupiik.fusion.http.server.api.Response;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.CompletionStage;
import java.util.logging.Logger;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

import static io.yupiik.fusion.framework.build.api.http.HttpMatcher.PathMatching.STARTS_WITH;
import static java.util.Optional.ofNullable;
import static java.util.logging.Level.SEVERE;
import static java.util.stream.Collectors.joining;

@ApplicationScoped
public class MetricsRelayEndpoint {

    private final Logger logger = Logger.getLogger(MetricsRelayEndpoint.class.getName());

    private final MetricsRelayStorage storage;

    public MetricsRelayEndpoint(MetricsRelayStorage storage) {
        this.storage = storage;
    }

    @HttpMatcher(path = "/relay", methods = "POST", pathMatching = STARTS_WITH)
    public CompletionStage<Response> storeMetrics(final Request request) {
        final String responseContentType = "text/plain";
        final boolean dropOnPull = Boolean.parseBoolean(ofNullable(request.parameter("dropOnPull")).orElse("false"));
        final String id = ofNullable(request.parameter("id")).orElse("");

        return read(request)
                .thenApply(bufferedReader -> {
                    try (bufferedReader) {
                        this.storage.store(id, bufferedReader.lines().collect(joining("\n")).strip(), dropOnPull);
                        return Response.of().body("OK").status(201).header("Content-Type", responseContentType).build();
                    } catch (IOException exception) {
                        logger.log(SEVERE, exception, exception::getMessage);
                        return Response.of().body("KO").header("Content-Type", responseContentType).build();
                    }
                })
                .exceptionally(throwable -> {
                    logger.log(SEVERE, throwable, throwable::getMessage);
                    return Response.of().body("KO").header("Content-Type", responseContentType).build();
                });
    }

    @HttpMatcher(path = "/relay", methods = "GET", pathMatching = STARTS_WITH)
    public Response fetchMetrics(final Request request) {
        final String responseContentType = "application/openmetrics-text; version=1.0.0; charset=utf-8";
        if (this.storage.getMetrics().isEmpty()) {
            return Response.of().header("Content-Type", responseContentType).build();
        }

        final boolean ignoreDrop = Boolean.parseBoolean(ofNullable(request.parameter("ignoreDrop")).orElse("false"));
        final String payload = Stream.of(storage.getMetrics().toArray(new MetricsRelayStorage.Entry[0]))
                .peek(entry -> {
                    if (!ignoreDrop && entry.dropOnPull()) {
                        this.storage.remove(entry); // it is a CopyOnWriteArrayList so it.remove() does not work
                    }
                })
                .map(MetricsRelayStorage.Entry::content).sorted().collect(joining("\n", "", "\n# EOF\n"));

        return Response.of().body(payload).status(200).header("Content-Type", responseContentType).build();
    }

    private CompletionStage<BufferedReader> read(final Request request) {
        final var encoding = request.header("content-encoding");
        return request.fullBody().bytes()
                .thenApply(
                    ByteArrayInputStream::new
                ).thenApply(byteArrayIs -> {
                        if (encoding != null && encoding.contains("gzip")) { // our client sends in gzip depending a limit
                            try {
                                return new BufferedReader(new InputStreamReader(new GZIPInputStream(byteArrayIs)));
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        } else {
                            return new BufferedReader(new InputStreamReader(byteArrayIs));
                        }
                    }
                );
    }
}
