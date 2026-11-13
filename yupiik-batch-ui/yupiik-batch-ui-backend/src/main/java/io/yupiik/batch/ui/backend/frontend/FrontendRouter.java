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
    private static final String EXTENSIONS_MARKER = "<!--yupiik-batch-extensions-->";

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
        final int marker = indexHtml.indexOf(EXTENSIONS_MARKER);
        if (marker < 0) {
            throw new IllegalArgumentException("Missing frontend extensions marker: " + EXTENSIONS_MARKER);
        }
        if (indexHtml.indexOf(EXTENSIONS_MARKER, marker + EXTENSIONS_MARKER.length()) >= 0) {
            throw new IllegalArgumentException("Duplicate frontend extensions marker: " + EXTENSIONS_MARKER);
        }
        final var js = configuration.getFrontendExtensionsJs();
        final var extension = js == null || js.isBlank() ? "" :
                "<script defer=\"defer\" src=\"" + js.replace("&", "&amp;")
                        .replace("\"", "&quot;")
                        .replace("'", "&#39;")
                        .replace("<", "&lt;")
                        .replace(">", "&gt;") + "\"></script>";
        return (indexHtml.substring(0, marker) + extension +
                indexHtml.substring(marker + EXTENSIONS_MARKER.length())).getBytes(StandardCharsets.UTF_8);
    }
}
