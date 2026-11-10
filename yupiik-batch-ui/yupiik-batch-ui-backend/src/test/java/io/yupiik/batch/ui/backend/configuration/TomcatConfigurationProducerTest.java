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
package io.yupiik.batch.ui.backend.configuration;

import io.yupiik.uship.webserver.tomcat.TomcatWebServer;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Standalone Tomcat (not the shared CDI one) since web users must be set before the configuration is bound.
 */
class TomcatConfigurationProducerTest {
    private final HttpClient client = HttpClient.newHttpClient();

    @Test
    void basicAuthenticationWhenWebUsersAreSet() throws Exception {
        final var tomcatConfiguration = new TomcatConfigurationProducer().configuration(configuration("admin=secret"));
        tomcatConfiguration.setPort(0);
        tomcatConfiguration.setInitializers(List.of((classes, context) -> context
                .addServlet("ok", new HttpServlet() {
                    @Override
                    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
                        resp.getWriter().write("ok " + req.getRemoteUser());
                    }
                })
                .addMapping("/ok")));

        try (final var server = new TomcatWebServer(tomcatConfiguration).create()) {
            final var anonymous = get(server.getPort(), null);
            assertEquals(401, anonymous.statusCode());
            assertTrue(anonymous.headers().firstValue("www-authenticate").orElse("").startsWith("Basic "), anonymous.headers()::toString);

            assertEquals(401, get(server.getPort(), "admin:wrong").statusCode());

            final var authenticated = get(server.getPort(), "admin:secret");
            assertEquals(200, authenticated.statusCode());
            assertEquals("ok admin", authenticated.body());
        }
    }

    @Test
    void noAuthenticationByDefault() throws Exception {
        final var tomcatConfiguration = new TomcatConfigurationProducer().configuration(configuration(null));
        assertTrue(tomcatConfiguration.getContextCustomizers() == null || tomcatConfiguration.getContextCustomizers().isEmpty());
    }

    private Configuration configuration(final String webUsers) throws Exception {
        final var keys = new String[]{
                "yupiik.batch.backend.tomcat.webUsers", "yupiik.batch.backend.datasource.driver", "yupiik.batch.backend.datasource.url"};
        final var previous = new String[keys.length];
        for (int i = 0; i < keys.length; i++) {
            previous[i] = System.getProperty(keys[i]);
        }
        try {
            if (webUsers == null) {
                System.clearProperty(keys[0]);
            } else {
                System.setProperty(keys[0], webUsers);
            }
            System.setProperty(keys[1], "org.h2.Driver");
            System.setProperty(keys[2], "jdbc:h2:mem:auth");
            final var configuration = new Configuration();
            final var init = Configuration.class.getDeclaredMethod("init");
            init.setAccessible(true);
            init.invoke(configuration);
            return configuration;
        } finally {
            for (int i = 0; i < keys.length; i++) {
                if (previous[i] == null) {
                    System.clearProperty(keys[i]);
                } else {
                    System.setProperty(keys[i], previous[i]);
                }
            }
        }
    }

    private HttpResponse<String> get(final int port, final String credentials) throws IOException, InterruptedException {
        final var request = HttpRequest.newBuilder().GET().uri(URI.create("http://localhost:" + port + "/ok"));
        if (credentials != null) {
            request.header("Authorization", "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(UTF_8)));
        }
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString(UTF_8));
    }
}
