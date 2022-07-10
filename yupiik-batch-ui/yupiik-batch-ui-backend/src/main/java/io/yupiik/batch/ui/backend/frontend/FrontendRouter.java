/*
 * Copyright (c) 2021-2022 - Yupiik SAS - https://www.yupiik.com
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
package io.yupiik.batch.ui.backend.frontend;

import io.yupiik.batch.ui.backend.configuration.Configuration;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static java.util.Objects.requireNonNull;

@Dependent
public class FrontendRouter extends HttpServlet {
    @Inject
    private Configuration configuration;

    private byte[] indexHtmlBytes;

    @Override
    public void init(final ServletConfig config) {
        final var out = new ByteArrayOutputStream();
        try (final var in = config.getServletContext().getClassLoader()
                .getResourceAsStream("META-INF/resources/index.html")) {
            requireNonNull(in, "didn't find index.html").transferTo(out);
        } catch (final IOException e) {
            throw new IllegalArgumentException(e);
        }
        indexHtmlBytes = rewrite(out.toString(StandardCharsets.UTF_8));
    }

    // todo: handle etag, cache etc, not super important for this kind particular endpoint and kind of app, for now it is ok like that
    @Override
    public void doGet(final HttpServletRequest servletRequest, final HttpServletResponse servletResponse) throws IOException {
        servletResponse.setBufferSize(indexHtmlBytes.length);
        servletResponse.setContentType("text/html");
        try (final var out = servletResponse.getOutputStream()) {
            out.write(indexHtmlBytes);
        }
    }

    private byte[] rewrite(final String indexHtml) {
        final var js = configuration.getFrontendExtensionsJs();
        if (js != null && !js.isBlank()) {
            final int start = indexHtml.indexOf("<script src=\"/static/js/main.");
            if (start < 0) {
                throw new IllegalArgumentException("Unexpected html");
            }
            return (indexHtml.substring(0, start) +
                    "<script src=\"" + js + "\"></script>" +
                    indexHtml.substring(start)).getBytes(StandardCharsets.UTF_8);
        }
        return indexHtml.getBytes(StandardCharsets.UTF_8);
    }
}
