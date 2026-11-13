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
package io.yupiik.batch.metricsrelay;

import io.yupiik.fusion.framework.api.configuration.ConfigurationSource;
import io.yupiik.fusion.framework.api.container.configuration.source.EnvironmentSource;
import io.yupiik.fusion.framework.api.container.configuration.source.SystemPropertiesSource;
import io.yupiik.fusion.framework.api.scope.DefaultScoped;

/**
 * Keeps the monitoring server (/health, /metrics) enabled on port 8181 by default as it was with fusion-observability
 * (fusion &lt; 1.1), fusion now disables it by default and uses port 8081.
 * The legacy {@code fusion.observability.server.port} key is still honored.
 * Explicit system properties or environment variables always win since this source only provides defaults.
 */
@DefaultScoped
public class MonitoringDefaultsSource implements ConfigurationSource {
    private static final String ENABLED = "fusion.http-server.monitoring.enabled";
    private static final String PORT = "fusion.http-server.monitoring.port";
    private static final String LEGACY_PORT = "fusion.observability.server.port";

    private final ConfigurationSource systemProperties = new SystemPropertiesSource();
    private final ConfigurationSource environment = new EnvironmentSource();

    @Override
    public String get(final String key) {
        return switch (key) {
            case ENABLED -> explicit(ENABLED) == null ? "true" : null;
            case PORT -> {
                if (explicit(PORT) != null) {
                    yield null;
                }
                final var legacy = explicit(LEGACY_PORT);
                yield legacy == null ? "8181" : legacy;
            }
            default -> null;
        };
    }

    private String explicit(final String key) {
        final var value = systemProperties.get(key);
        return value == null ? environment.get(key) : value;
    }
}
