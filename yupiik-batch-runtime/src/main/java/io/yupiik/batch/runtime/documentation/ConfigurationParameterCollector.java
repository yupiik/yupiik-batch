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
package io.yupiik.batch.runtime.documentation;

import io.yupiik.batch.runtime.batch.Batch;
import io.yupiik.batch.runtime.batch.Batches;
import io.yupiik.batch.runtime.configuration.Binder;
import io.yupiik.batch.runtime.configuration.Param;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.stream.Collectors.toMap;

@Deprecated // use simple-configuration one
public record ConfigurationParameterCollector(
        List<Class<?>> batchClasses,
        BiPredicate<Object, String> isNested) implements Supplier<Map<String, ConfigurationParameterCollector.Parameter>> {
    public ConfigurationParameterCollector(final List<Class<?>> batchClasses) {
        this(batchClasses, null);
    }

    @Override
    public Map<String, Parameter> get() {
        return mapParams(collector().get());
    }

    public String toAsciidoc() {
        return collector().toAsciidoc();
    }

    public Map<String, Parameter> getWithPrefix(final Function<Class<?>, String> prefix) {
        return mapParams(collector().getWithPrefix(prefix));
    }

    private io.yupiik.batch.runtime.configuration.documentation.ConfigurationParameterCollector collector() {
        return new io.yupiik.batch.runtime.configuration.documentation.ConfigurationParameterCollector(mapClasses()) {
            @Override
            protected Binder visitor(final String prefix, final Map<String, Parameter> doc) {
                return new io.yupiik.batch.runtime.batch.Binder(prefix, List.of()) { // handle both @Param
                    @Override
                    protected void doBind(final Object instance, final Field param, final Param conf, final String paramName) {
                        onParam(instance, param, conf, paramName, this::isList, this::isNestedModel, this::newNestedBinder, doc);
                    }

                    @Override
                    protected boolean isNestedModel(final Object instance, final String fieldType) {
                        return super.isNestedModel(instance, fieldType) || (isNested != null && isNested.test(instance, fieldType));
                    }
                };
            }
        };
    }

    private Map<String, Parameter> mapParams(final Map<String, io.yupiik.batch.runtime.configuration.documentation.ConfigurationParameterCollector.Parameter> params) {
        return params.entrySet().stream()
                .collect(toMap(Map.Entry::getKey, p -> new Parameter(
                        p.getValue().param(), p.getValue().defaultValue(), p.getValue().type(), p.getValue().name())));
    }

    private List<Class<?>> mapClasses() {
        return List.of(
                batchClasses.stream()
                        .map(batchType -> (Class<?>) (Batch.class.isAssignableFrom(batchType) ?
                                Batches.findConfType(Class.class.cast(batchType)) : batchType))
                        .toArray(Class[]::new));
    }

    public record Parameter(Param param, String defaultValue, Type type, String name) {
    }
}
