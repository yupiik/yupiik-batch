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

import io.yupiik.batch.runtime.documentation.IteratorDescription;
import org.apache.xbean.finder.AnnotationFinder;
import org.apache.xbean.finder.ClassLoaders;
import org.apache.xbean.finder.archive.Archive;
import org.apache.xbean.finder.archive.CompositeArchive;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.apache.xbean.finder.archive.ClasspathArchive.archive;
import static org.apache.xbean.finder.archive.FileArchive.decode;

public record DocumentIterators(Path sourceBase) implements Runnable {
    @Override
    public void run() {
        final var target = sourceBase.resolve("content/generated/iterators.adoc");
        final var jarMatcher = Pattern.compile("[a-z0-1A-Z]+-iterator-\\p{Digit}\\.\\p{Digit}+\\.\\p{Digit}+(-SNAPSHOT)?.jar").asMatchPredicate();
        try {
            final var archive = new CompositeArchive(ClassLoaders.findUrls(Thread.currentThread().getContextClassLoader()).stream()
                    .filter(it -> jarMatcher.test(org.apache.xbean.finder.util.Files.toFile(it).getName()))
                    .map(it -> archive(Thread.currentThread().getContextClassLoader(), it))
                    .toArray(Archive[]::new));

            Files.createDirectories(target.getParent());
            Files.writeString(target, new AnnotationFinder(archive, false).findAnnotatedClasses(IteratorDescription.class).stream()
                    .map(this::document)
                    .collect(joining("\n\n")));
        } catch (final IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private String document(final Class<?> iterator) {
        final var factories = findFactories(iterator);
        return "== " + iterator.getName() + "\n" +
                "\n" +
                iterator.getAnnotation(IteratorDescription.class).value().strip() + "\n" +
                "\n" +
                (factories.isEmpty() ? "" : "=== Factories\n" +
                        "\n" +
                        String.join("\n", factories) + "\n") +
                "\n" +
                "=== Dependency\n" +
                "\n" +
                "[source,xml]\n" +
                "----\n" +
                "<dependency>\n" +
                "  <groupId>io.yupiik.batch</groupId>\n" +
                "  <artifactId>" + toArtifactId(jar(iterator)) + "</artifactId>\n" +
                "  <version>${yupiik-batch.version}</version>\n" +
                "</dependency>\n" +
                "----\n" +
                "\n";
    }

    private List<String> findFactories(final Class<?> iterator) {
        return Stream.of(iterator.getMethods())
                .filter(it -> Modifier.isStatic(it.getModifiers()) && Modifier.isPublic(it.getModifiers()))
                .filter(it -> it.isAnnotationPresent(IteratorDescription.Factory.class))
                .map(it -> "==== " + it.toGenericString()
                        .replace(iterator.getPackageName() + '.', "")
                        .replace(iterator.getSimpleName() + '$', "")
                        .replace("java.util.", "")
                        .replace("java.lang.", "")
                        .replace("java.nio.file.", "")
                        .replace("public static ", "") + "\n" +
                        "\n" +
                        it.getAnnotation(IteratorDescription.Factory.class).value().strip() + "\n")
                .sorted()
                .collect(toList());
    }

    public static String toArtifactId(final Path path) {
        final var name = path.getFileName().toString();
        return name.substring(0, name.indexOf("-iterator")) + "-iterator";
    }

    public static Path jar(final Class<?> clazz) {
        try {
            final var url = Thread.currentThread().getContextClassLoader()
                    .getResource(clazz.getName().replace('.', '/') + ".class");
            if (url == null) {
                throw new IllegalStateException("missing " + clazz);
            }
            return switch (url.getProtocol()) {
                case "jar" -> {
                    final var spec = url.getFile();
                    final int separator = spec.indexOf('!');
                    if (separator == -1) {
                        throw new IllegalArgumentException("Missing ! in " + url);
                    }
                    yield Paths.get(decode(new URL(spec.substring(0, separator)).getFile()));
                }
                case "file" -> Paths.get(decode(url.getFile()));
                default -> throw new IllegalArgumentException(url.toExternalForm());
            };
        } catch (final RuntimeException e) {
            throw e;
        } catch (final Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
