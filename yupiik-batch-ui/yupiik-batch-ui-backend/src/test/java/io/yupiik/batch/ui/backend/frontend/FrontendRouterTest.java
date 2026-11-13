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
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Tests use local instances and resources, never the shared CDI container or system properties. */
class FrontendRouterTest {
    private static final String MARKER = "<!--yupiik-batch-extensions-->";
    private static final String SCRIPTS = "<script defer src=\"/static/js/runtime.js\"></script>" +
            "<script defer=\"defer\" src=\"/static/js/vendors.js\"></script>" +
            "<script defer src=\"/static/js/index.js\"></script>";
    private static final String HTML = "<!doctype html><html><head>" + MARKER + SCRIPTS +
            "</head><body><div id=\"root\"></div></body></html>";

    @Test
    void insertsDeferredExtensionBeforeAllBundles() throws Exception {
        assertEquals(HTML.replace(MARKER, "<script defer=\"defer\" src=\"/extensions/custom.js\"></script>"),
                render(HTML, "/extensions/custom.js"));
    }

    @Test
    void escapesExtensionAttributeWithoutChangingSurroundingHtml() throws Exception {
        assertEquals(HTML.replace(MARKER,
                        "<script defer=\"defer\" src=\"/custom.js?a=1&amp;b=&quot;&lt;tag&gt;&#39;&amp;quot;\"></script>"),
                render(HTML, "/custom.js?a=1&b=\"<tag>'&quot;"));
    }

    @Test
    void removesMarkerWhenExtensionsAreDisabled() throws Exception {
        for (final var extension : new String[]{null, "", " \t\n"}) {
            assertEquals(HTML.replace(MARKER, ""), render(HTML, extension));
        }
    }

    @Test
    void requiresExactMarkerEvenWhenExtensionsAreDisabled() {
        for (final var extension : new String[]{null, "", " \t", "/custom.js"}) {
            for (final var html : new String[]{HTML.replace(MARKER, ""),
                    HTML.replace(MARKER, "<!-- yupiik-batch-extensions -->"),
                    "<script defer=\"defer\" src=\"/static/js/main.123.js\"></script>"}) {
                final var error = assertThrows(IllegalArgumentException.class, () -> render(html, extension));
                assertEquals("Missing frontend extensions marker: " + MARKER, error.getMessage());
            }
        }
    }

    @Test
    void rejectsDuplicateMarkersEvenWhenExtensionsAreDisabled() {
        for (final var extension : new String[]{null, "", " \t", "/custom.js"}) {
            final var error = assertThrows(IllegalArgumentException.class, () -> render(HTML + MARKER, extension));
            assertEquals("Duplicate frontend extensions marker: " + MARKER, error.getMessage());
        }
    }

    @Test
    void preservesUtf8() throws Exception {
        final var html = HTML.replace("<div id=\"root\"></div>", "<div id=\"root\">\u00e9</div>");
        assertEquals(html.replace(MARKER, ""), render(html, null));
    }

    private String render(final String html, final String extension) throws Exception {
        final var router = new FrontendRouter();
        final var configuration = FrontendRouter.class.getDeclaredField("configuration");
        configuration.setAccessible(true);
        configuration.set(router, new Configuration() {
            @Override
            public String getFrontendExtensionsJs() {
                return extension;
            }
        });
        final var loader = new ClassLoader(getClass().getClassLoader()) {
            @Override
            public InputStream getResourceAsStream(final String name) {
                assertEquals("META-INF/resources/index.html", name);
                return new ByteArrayInputStream(html.getBytes(UTF_8));
            }
        };
        final var context = proxy(ServletContext.class, (instance, method, args) -> {
            assertEquals("getClassLoader", method.getName());
            return loader;
        });
        router.init(proxy(ServletConfig.class, (instance, method, args) -> {
            assertEquals("getServletContext", method.getName());
            return context;
        }));
        final var bytes = new ByteArrayOutputStream();
        final var output = new ServletOutputStream() {
            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setWriteListener(final WriteListener listener) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void write(final int value) {
                bytes.write(value);
            }
        };
        router.doGet(null, proxy(HttpServletResponse.class, (instance, method, args) -> {
            switch (method.getName()) {
                case "setBufferSize":
                    return null;
                case "setContentType":
                    assertEquals("text/html", args[0]);
                    return null;
                case "getOutputStream":
                    return output;
                default:
                    throw new UnsupportedOperationException(method.getName());
            }
        }));
        return bytes.toString(UTF_8);
    }

    private <T> T proxy(final Class<T> type, final InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler));
    }
}
