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
package io.yupiik.batch.ui.backend.probes;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import javax.sql.DataSource;
import java.sql.SQLException;

public class Probes extends HttpServlet {
    private final DataSource dataSource;
    private final boolean live;

    public Probes(final DataSource dataSource, final boolean live) {
        this.dataSource = dataSource;
        this.live = live;
    }

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) {
        if (!live) {
            doReady(resp);
        } else {
            doLive(resp);
        }
    }

    private void doReady(final HttpServletResponse resp) {
        doLive(resp);
    }

    private void doLive(final HttpServletResponse resp) {
        try (final var c = dataSource.getConnection()) {
            if (c.isValid(30_000)) {
                resp.setStatus(200);
                return;
            }
        } catch (final SQLException throwables) {
            // no-op
        }
        resp.setContentType("text/plain");
        resp.setStatus(503);
    }
}
