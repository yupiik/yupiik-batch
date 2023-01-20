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
package io.yupiik.batch.documentation;

import io.yupiik.batch.runtime.documentation.Component;
import org.apache.xbean.finder.AnnotationFinder;
import org.apache.xbean.finder.archive.FileArchive;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.joining;

public record DocumentComponents(Path sourceBase) implements Runnable {
    @Override
    public void run() {
        final var target = sourceBase.resolve("content/generated/components.adoc");
        final var batchRuntimeClasses = sourceBase.getParent().getParent().getParent().getParent().resolve("yupiik-batch-runtime/target/classes");

        final var content = new AnnotationFinder(new FileArchive(
                Thread.currentThread().getContextClassLoader(), batchRuntimeClasses.toFile()))
                .findAnnotatedClasses(Component.class).stream()
                .sorted(comparing(Class::getName))
                .map(c -> "" +
                        "=== " + c.getName() + "\n" +
                        "\n" +
                        c.getAnnotation(Component.class).value().strip() + "\n")
                .collect(joining("\n"));

        try {
            Files.createDirectories(target.getParent());
            Files.writeString(target, content);
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
