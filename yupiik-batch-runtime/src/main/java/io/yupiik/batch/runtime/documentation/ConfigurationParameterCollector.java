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
package io.yupiik.batch.runtime.documentation;

import io.yupiik.batch.runtime.batch.Batch;
import io.yupiik.batch.runtime.batch.Batches;
import io.yupiik.batch.runtime.batch.Binder;
import io.yupiik.batch.runtime.configuration.Param;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.Locale.ROOT;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;

public record ConfigurationParameterCollector(
        List<Class<Batch<?>>> batchClasses,
        BiPredicate<Object, String> isNested) implements Supplier<Map<String, ConfigurationParameterCollector.Parameter>> {
    public ConfigurationParameterCollector(final List<Class<Batch<?>>> batchClasses) {
        this(batchClasses, null);
    }

    @Override
    public Map<String, Parameter> get() {
        return getWithPrefix(batchType -> batchType.getSimpleName().toLowerCase(Locale.ROOT));
    }

    public Map<String, Parameter> getWithPrefix(final Function<Class<?>, String> prefix) {
        return batchClasses.stream()
                .flatMap(batchType -> {
                    final var doc = new HashMap<String, Parameter>();
                    new Binder(prefix.apply(batchType), List.of()) {
                        @Override
                        protected void doBind(final Object instance, final Field param, final Param conf, final String paramName) {
                            onParam(instance, param, conf, paramName);
                        }

                        private void onParam(final Object instance, final Field param, final Param conf, final String paramName) {
                            if (isList(param) || !isNestedModel(instance, param.getType().getTypeName())) {
                                if (!param.canAccess(instance)) {
                                    param.setAccessible(true);
                                }
                                try {
                                    final var defValue = param.get(instance);
                                    doc.put(paramName, new Parameter(
                                            conf, defValue == null ? null : String.valueOf(defValue), param.getGenericType(),
                                            paramName));
                                } catch (final IllegalAccessException e) {
                                    throw new IllegalStateException(e);
                                }
                            } else { // recursive call
                                new Binder(paramName, List.of()) {
                                    @Override
                                    protected void doBind(final Object instance, final Field param, final Param conf, final String paramName) {
                                        onParam(instance, param, conf, paramName);
                                    }
                                }.bind(param.getType());
                            }
                        }

                        @Override
                        protected boolean isNestedModel(final Object instance, final String fieldType) {
                            return super.isNestedModel(instance, fieldType) || (isNested != null && isNested.test(instance, fieldType));
                        }
                    }.bind(Batches.findConfType(batchType));
                    return doc.entrySet().stream();
                })
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public String toAsciidoc() {
        return "[options=\"header\",cols=\"a,a,2\"]\n" +
                "|===\n" +
                "|Name|Env Variable|Description\n" +
                getWithPrefix(c -> "").entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .map(e -> "" +
                                "| `" + e.getKey() + "` " + (e.getValue().param().required() ? "*" : "") +
                                "| `" + e.getKey().replaceAll("[^A-Za-z0-9]", "_").toUpperCase(ROOT) + "` " +
                                "| " + e.getValue().param().description() + "\n")
                        .collect(joining()) + "\n" +
                "|===\n";
    }

    public static record Parameter(Param param, String defaultValue, Type type, String name) {
    }
}
