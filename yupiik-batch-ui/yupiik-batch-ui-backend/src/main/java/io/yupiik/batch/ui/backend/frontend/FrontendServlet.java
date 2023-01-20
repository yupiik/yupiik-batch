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
package io.yupiik.batch.ui.backend.frontend;

import jakarta.servlet.ServletException;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.servlets.DefaultServlet;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import static java.util.Collections.list;

public class FrontendServlet extends DefaultServlet {
    @Override
    public void init() throws ServletException {
        super.init();

        // add frontend to resources - not scanned in current Tomcat setup, avoids to add ContextConfig
        try {
            list(Thread.currentThread().getContextClassLoader().getResources("META-INF/resources/")).forEach(url -> {
                switch (url.getProtocol()) {
                    case "jar" -> {
                        final var file = url.getFile();
                        try {
                            url = new URL("jar:" + file.substring(0, file.contains("!/") ? file.indexOf("!/") + 2 : file.length()));
                        } catch (final MalformedURLException e) {
                            throw new IllegalArgumentException(e);
                        }
                        resources.createWebResourceSet(WebResourceRoot.ResourceSetType.RESOURCE_JAR, "/", url, "/META-INF/resources");
                    }
                    case "file" -> resources.createWebResourceSet(WebResourceRoot.ResourceSetType.RESOURCE_JAR, "/", url, "/");
                    default -> throw new IllegalArgumentException("Unsupported " + url);
                }
            });
        } catch (final IOException e) {
            throw new ServletException(e);
        }
    }
}
