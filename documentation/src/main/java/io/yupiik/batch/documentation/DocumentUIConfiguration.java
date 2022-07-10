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
package io.yupiik.batch.documentation;

import io.yupiik.batch.runtime.batch.Batch;
import io.yupiik.batch.runtime.documentation.ConfigurationParameterCollector;
import io.yupiik.batch.ui.backend.configuration.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public record DocumentUIConfiguration(Path sourceBase) implements Runnable {
    @Override
    public void run() {
        final var target = sourceBase.resolve("content/generated/ui.configuration.adoc");
        try {
            Files.createDirectories(target.getParent());
            Files.writeString(target, new ConfigurationParameterCollector(List.of(Class.class.cast(FakeDocGenBatch.class))).toAsciidoc());
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static class FakeDocGenBatch implements Batch<Configuration> {
        @Override
        public void accept(final Configuration configuration) {
            throw new UnsupportedOperationException();
        }
    }
}
