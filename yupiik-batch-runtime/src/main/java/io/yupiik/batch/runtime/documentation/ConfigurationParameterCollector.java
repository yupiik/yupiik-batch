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
import io.yupiik.batch.runtime.batch.Param;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

import static java.util.stream.Collectors.toMap;

public record ConfigurationParameterCollector(
        List<Class<Batch<?>>> batchClasses) implements Supplier<Map<String, ConfigurationParameterCollector.Parameter>> {
    @Override
    public Map<String, Parameter> get() {
        return batchClasses.stream()
                .flatMap(batchType -> {
                    final var doc = new HashMap<String, Parameter>();
                    new Binder(batchType.getSimpleName().toLowerCase(Locale.ROOT), List.of()) {
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
                                    doc.put(paramName, new Parameter(conf, defValue == null ? null : String.valueOf(defValue)));
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
                    }.bind(Batches.findConfType(batchType));
                    return doc.entrySet().stream();
                })
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public static record Parameter(Param param, String defaultValue) {
    }
}
