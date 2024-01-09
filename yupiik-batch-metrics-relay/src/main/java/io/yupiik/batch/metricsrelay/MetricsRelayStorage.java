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

import io.yupiik.fusion.framework.api.scope.ApplicationScoped;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class MetricsRelayStorage {
    private final Map<String, Entry> metrics = new ConcurrentHashMap<>();

    public void store(final String id, final String content, final boolean dropOnPull) {
        metrics.put(id.isBlank() ? UUID.randomUUID().toString() : id, new Entry(content, dropOnPull || id.isBlank()));
    }

    public Collection<Entry> getMetrics() {
        return metrics.values();
    }

    public void remove(final Entry entry) {
        metrics.values().remove(entry);
    }

    public void clear() {
        metrics.clear();
    }

    public record Entry(String content, boolean dropOnPull) {
    }
}
