/*
 * Copyright (c) 2021 - Yupiik SAS - https://www.yupiik.com
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

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.ServletContext;

import java.util.Set;

@Dependent
public class Frontend implements ServletContainerInitializer {
    @Inject
    private FrontendRouter frontendServlet;

    @Override
    public void onStartup(final Set<Class<?>> set, final ServletContext servletContext) {
        final var servlet = servletContext.addServlet("frontend", FrontendServlet.class);
        servlet.setLoadOnStartup(1);
        servlet.setAsyncSupported(true);
        servlet.addMapping("/static/*", "/favicon.ico", "/asset-manifest.json", "/robots.txt");

        final var router = servletContext.addServlet("frontend-router", frontendServlet);
        router.setLoadOnStartup(1);
        router.setAsyncSupported(true);
        router.addMapping("/", "/index.html", "/home", "/execution/*", "/executions", "/extensions/*");
    }
}
