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
package io.yupiik.batch.ui.backend.probes;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.ServletContext;

import javax.sql.DataSource;
import java.util.Set;

@Dependent
public class ProbesRegistration implements ServletContainerInitializer {
    @Inject
    private DataSource dataSource;

    @Override
    public void onStartup(final Set<Class<?>> set, final ServletContext servletContext) {
        add(servletContext, new Probes(dataSource, true), "/api/health/live");
        add(servletContext, new Probes(dataSource, false), "/api/health/ready");
    }

    private void add(final ServletContext servletContext, final Probes probes, final String mapping) {
        final var servlet = servletContext.addServlet(mapping.substring(1).replace('/', '-'), probes);
        servlet.setLoadOnStartup(1);
        servlet.setAsyncSupported(true);
        servlet.addMapping(mapping);
    }
}
