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
package io.yupiik.batch.runtime.batch;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;

public class Binder {
    private final String prefix;
    private final List<String> args;

    public Binder(final String prefix, final List<String> args) {
        this.prefix = prefix;
        this.args = args;
    }

    public <T> T bind(final Class<T> instance) {
        try {
            return bind(instance.getConstructor().newInstance());
        } catch (final InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public <T> T bind(final T instance) {
        bind(instance, instance.getClass());
        return instance;
    }

    private void bind(final Object instance, final Class<?> type) {
        if (type == Object.class || type == null) {
            return;
        }
        Stream.of(type.getDeclaredFields())
                .filter(it -> it.isAnnotationPresent(Param.class))
                .sorted(comparing(it -> toName(it, it.getAnnotation(Param.class))))
                .forEach(param -> {
                    final var conf = param.getAnnotation(Param.class);
                    final var paramName = toName(param, conf);
                    doBind(instance, param, conf, paramName);
                });
        bind(instance, type.getSuperclass());
    }

    protected String toName(final Field param, final Param conf) {
        return (prefix == null || prefix.isBlank() ? "" : prefix + "-") + (conf.name().isBlank() ? param.getName() : conf.name());
    }

    protected void doBind(final Object instance, final Field param, final Param conf, final String paramName) {
        final var value = findParam(paramName);
        final Object toSet;
        if (isList(param)) {
            final var list = value.map(it -> coerce(it, param.getType())).collect(toList());
            if (list.isEmpty() && conf.required()) {
                throw new IllegalArgumentException("Missing parameter --" + paramName);
            }
            toSet = list;
        } else { // singular value
            final var fieldType = param.getType().getTypeName();
            if (isNestedModel(instance, fieldType)) {
                toSet = new Binder(paramName, args) {
                    @Override // enables to keep the overriden behavior when the root binder is a child
                    protected void doBind(final Object instance, final Field param, final Param conf, final String paramName) {
                        Binder.this.doBind(instance, param, conf, paramName);
                    }
                }.bind(param.getType());
            } else { // "primitive"
                toSet = value.findFirst()
                        .map(it -> coerce(it, param.getType()))
                        .orElseGet(() -> {
                            final var env = System.getenv(paramName.replaceAll("[^A-Za-z0-9]", "_").toUpperCase(Locale.ROOT));
                            if (env != null) {
                                return coerce(env, param.getType());
                            }
                            if (conf.required()) {
                                throw new IllegalArgumentException("Missing parameter --" + paramName);
                            }
                            // let's keep field's default
                            return null;
                        });
            }
        }
        if (toSet != null) {
            if (!param.canAccess(instance)) {
                param.setAccessible(true);
            }
            try {
                param.set(instance, toSet);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    protected boolean isList(final Field param) {
        return ParameterizedType.class.isInstance(param.getGenericType()) &&
                List.class == ParameterizedType.class.cast(param.getGenericType()).getRawType();
    }

    // todo: relax?
    protected boolean isNestedModel(final Object instance, final String fieldType) {
        return fieldType.startsWith(instance.getClass().getPackageName()) || // assume nested classes are in the same package
                fieldType.equals("io.yupiik.batch.runtime.sql.DataSourceConfiguration");
    }

    private Object coerce(final String value, final Class<?> type) {
        if (type == boolean.class || type == Boolean.class) {
            return Boolean.parseBoolean(value);
        }
        if (type == byte.class || type == Byte.class) {
            return Byte.parseByte(value);
        }
        if (type == short.class || type == Short.class) {
            return Short.parseShort(value);
        }
        if (type == int.class || type == Integer.class) {
            return Integer.parseInt(value);
        }
        if (type == long.class || type == Long.class) {
            return Long.parseLong(value);
        }
        if (type == double.class || type == Double.class) {
            return Double.parseDouble(value);
        }
        if (type == float.class || type == Float.class) {
            return Float.parseFloat(value);
        }
        if (type == String.class || type == Object.class) {
            return value;
        }
        if (type == Path.class) {
            return Paths.get(value);
        }
        throw new IllegalArgumentException("Unsupported parameter type: " + type);
    }

    private Stream<String> findParam(final String name) {
        final var result = new ArrayList<String>();
        final var neg = "--no-" + name;
        final var expected = List.of("--" + name, "-" + name);
        for (int i = 0; i < args.size(); i++) {
            final var current = args.get(i);
            if (expected.contains(current)) {
                if (args.size() - 1 == i) {
                    result.add("false");
                } else {
                    result.add(args.get(i + 1));
                    i++;
                }
            } else if (neg.equals(current)) {
                result.add("false");
            }
        }
        return result.stream();
    }
}