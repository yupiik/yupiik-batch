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
package io.yupiik.batch.runtime.component;

import io.yupiik.batch.runtime.documentation.Component;
import io.yupiik.batch.runtime.component.mapping.Mapping;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.Comparator.comparing;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

// todo: nested objects support if needed (mapper of mapper in terms of impl, nothing more crazy)
@Component("""
        This mapping component enables to convert an input to an output instance by providing an specification instance.
                
        It is a class decorated with `@Mapping`:
                
        [source,java]
        ----
        @Mapping(
                from = IncomingModel.class,
                to = OutputModel.class,
                documentation = "Converts an input to an output.",
                properties = {
                        @Property(type = CONSTANT, to = "outputFieldUrl", value = "https://foo.bar/"),
                        @Property(type = TABLE_MAPPING, from = "inputKeyField", to = "mappedOutput", value = "myLookupTable", onMissedTableLookup = FORWARD)
                },
                tables = {
                        @Mapping.MappingTable(
                                name = "myLookupTable",
                                entries = {
                                        @Entry(input = "A", output = "1"),
                                        @Entry(input = "C", output = "3")
                                }
                        )
                })
        public class MyMapperSpec {
            @Mapping.Custom(description = "This will map X to Y.")
            String outputField(final IncomingModel in[, @Table("myLookupTable") final Map<String, String> myLookupTable) {
                return ...;
            }
        }
        ----
        
        To get a mapper, you simply call `Mapper.mapper(MyMapperSpec.class)` and then can insert this mapper in any `BatchChain`.
        
        The specification API enables static mapping (`properties`) or custom mapping - `@Mapping.Custom` - for more advanced logic.
        
        The companion class `io.yupiik.batch.runtime.documentation.MapperDocGenerator` enables to generate an asciidoctor documentation for a mapper class.""")
public class Mapper<A, B, C> implements Function<A, B> {
    private final Function<A, B> delegate;
    private final C delegateInstance;

    public Mapper(final Class<C> spec) {
        if (!spec.isInterface() && !Modifier.isAbstract(spec.getModifiers())) {
            C instance = null;
            try {
                instance = spec.getConstructor().newInstance();
            } catch (final NoSuchMethodException nsme) {
                // no-op
            } catch (final InstantiationException | IllegalAccessException e) {
                throw new IllegalStateException(e);
            } catch (final InvocationTargetException e) {
                throw new IllegalStateException(e.getTargetException());
            }
            delegateInstance = instance;
        } else {
            delegateInstance = null;
        }
        this.delegate = createMapper(spec);
    }

    public C getDelegateInstance() {
        return delegateInstance;
    }

    @Override
    public B apply(final A a) {
        return delegate.apply(a);
    }

    private Function<A, B> createMapper(final Class<?> spec) {
        final var conf = spec.getAnnotation(Mapping.class);
        if (conf == null) {
            throw new IllegalArgumentException("No @Mapping on " + spec);
        }

        final var from = conf.from();
        final var to = conf.to();

        final var tableMappings = collectMappingTables(conf);

        final var toConstructor = Stream.of(to.getDeclaredConstructors())
                .max(comparing(Constructor::getParameterCount)) // a bit random but likely works for "pure" records
                .map(c -> {
                    c.trySetAccessible();
                    return c;
                })
                .orElseThrow(() -> new IllegalArgumentException("No constructor for " + to));
        final var toOrderedProperties = Stream.of(toConstructor.getParameters()).collect(toList());

        final var mappers = collectMappers(spec, conf, from, tableMappings, toOrderedProperties);

        // prepare mapper in order + add missing one (default value)
        final var orderedMappers = toOrderedProperties.stream()
                .map(it -> ofNullable(mappers.get(it.getName()))
                        .orElseGet(() -> {
                            final var type = it.getType();
                            if (type.isPrimitive()) {
                                return toPrimitiveDefaultMapper(type);
                            }
                            return i -> null;
                        }))
                .collect(toList());

        return input -> {
            if (!from.isInstance(input)) {
                throw new IllegalArgumentException("Unsupported input: " + input + ", expected: " + from);
            }
            try {
                final var params = orderedMappers.stream()
                        .map(it -> it.apply(input))
                        .toArray(Object[]::new);
                final var newInstance = toConstructor.newInstance(params);
                return (B) newInstance;
            } catch (final InstantiationException | IllegalAccessException e) {
                throw new IllegalStateException(e);
            } catch (final InvocationTargetException e) {
                throw new IllegalStateException(e.getTargetException());
            }
        };
    }

    private Map<String, Function<Object, Object>> collectMappers(final Class<?> spec, final Mapping conf,
                                                                 final Class<?> from,
                                                                 final Map<String, Map<String, String>> tableMappings,
                                                                 final List<Parameter> toOrderedProperties) {
        return Stream.of(
                Stream.of(conf.properties())
                        .peek(c -> c.type().validate(c))
                        .collect(toMap(Mapping.Property::to, c -> toPropertyMapper(c, toOrderedProperties, tableMappings))),
                findMethods(spec)
                        .collect(toMap(m -> {
                            final var target = m.getAnnotation(Mapping.Custom.class).to();
                            return target.isEmpty() ? m.getName() : target;
                        }, i -> toCustomPropertyMapper(from, i, tableMappings))))
                .flatMap(m -> m.entrySet().stream())
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Map<String, Map<String, String>> collectMappingTables(final Mapping conf) {
        return Stream.of(conf.tables())
                .collect(toMap(Mapping.MappingTable::name, mt -> Stream.of(mt.entries())
                        .collect(toMap(Mapping.Entry::input, Mapping.Entry::output))));
    }

    private Stream<Method> findMethods(final Class<?> spec) {
        if (spec == null || spec == Object.class) {
            return Stream.empty();
        }
        return Stream.concat(
                Stream.of(spec.getDeclaredMethods())
                        .filter(m -> m.isAnnotationPresent(Mapping.Custom.class)),
                findMethods(spec.getSuperclass()));
    }

    private Function<Object, Object> toPrimitiveDefaultMapper(final Class<?> type) {
        if (type == boolean.class) {
            return i -> false;
        }
        if (type == float.class) {
            return i -> 0.f;
        }
        if (type == double.class) {
            return i -> 0.;
        }
        if (type == int.class) {
            return i -> 0;
        }
        if (type == long.class) {
            return i -> 0L;
        }
        if (type == short.class) {
            final var v = (short) 0;
            return i -> v;
        }
        if (type == byte.class) {
            final var v = (byte) 0;
            return i -> v;
        }
        throw new IllegalArgumentException("Unsupported primitive: " + type);
    }

    private <I> Function<I, Object> toCustomPropertyMapper(final Class<?> inputType, final Method method,
                                                           final Map<String, Map<String, String>> tables) {
        if (!method.trySetAccessible()) {
            throw new IllegalArgumentException("Can't access " + method);
        }

        return switch (method.getParameterCount()) {
            case 0 -> input -> {
                try {
                    return method.invoke(delegateInstance);
                } catch (final IllegalAccessException e) {
                    throw new IllegalStateException(e);
                } catch (final InvocationTargetException e) {
                    throw new IllegalStateException("Error invoking " + method, e.getTargetException());
                }
            };
            default -> {
                final var parametersFactory = Stream.of(method.getParameters())
                        .map(it -> {
                            if (inputType == Object.class || it.getType().isAssignableFrom(inputType)) {
                                return (Function<Object, Object>) i -> i;
                            }
                            if (it.isAnnotationPresent(Mapping.Table.class)) {
                                final var name = it.getAnnotation(Mapping.Table.class).value();
                                final var mapping = tables.get(name);
                                if (mapping == null) {
                                    throw new IllegalArgumentException("No mapping table named '" + name + "'");
                                }
                                if (Mapping.ReversedTable.class == it.getType()) {
                                    final var value = new Mapping.ReversedTable(mapping.entrySet());
                                    return (Function<Object, Object>) i -> value;
                                }
                                return (Function<Object, Object>) i -> mapping;
                            }
                            throw new IllegalArgumentException("Unsupported parameter: " + it.getName() + " in " + method);
                        })
                        .collect(toList());
                yield input -> {
                    final var params = parametersFactory.stream()
                            .map(it -> it.apply(input))
                            .toArray(Object[]::new);
                    try {
                        return method.invoke(delegateInstance, params);
                    } catch (final IllegalAccessException e) {
                        throw new IllegalStateException(e);
                    } catch (final InvocationTargetException e) {
                        throw new IllegalStateException("Error invoking " + method + " with parameters " + List.of(params), e.getTargetException());
                    }
                };
            }
        };
    }

    private <I> Function<I, Object> toPropertyMapper(final Mapping.Property property,
                                                     final List<Parameter> toOrderedProperties,
                                                     final Map<String, Map<String, String>> tableMappings) {
        return switch (property.type()) {
            case MAPPED -> new MappedMapper<>(property.from(), a -> a);
            case TABLE_MAPPING -> {
                final var ref = tableMappings.get(property.value());
                if (ref == null) {
                    throw new IllegalArgumentException("No table mapping named '" + property.value() + "'");
                }
                yield switch (property.onMissedTableLookup()) {
                    case NULL -> new MappedMapper<>(property.from(), a -> a == null ? null : ref.get(String.valueOf(a)));
                    case FORWARD -> new MappedMapper<>(property.from(), a -> {
                        if (a == null) {
                            return null;
                        }
                        final var k = String.valueOf(a);
                        return ref.getOrDefault(k, k);
                    });
                    case FAIL -> new MappedMapper<>(property.from(), a -> {
                        if (a == null) {
                            throw new IllegalArgumentException("No mapping for null in '" + property.value() + "'");
                        }
                        final var k = String.valueOf(a);
                        final var value = ref.get(k);
                        if (value == null) {
                            throw new IllegalArgumentException("No mapping for null in '" + property.value() + "'");
                        }
                        return value;
                    });
                };
            }
            case CONSTANT -> {
                final var value = property.value();
                final var type = toOrderedProperties.stream()
                        .filter(it -> property.to().equals(it.getName()))
                        .findFirst()
                        .map(Parameter::getType)
                        .orElseThrow(() -> new IllegalArgumentException("No property '" + property.to() + "' found"));
                if (type == String.class) {
                    yield i -> value;
                }
                if (type == int.class || type == Integer.class) {
                    final var parsed = Integer.parseInt(value);
                    yield i -> parsed;
                }
                if (type == long.class || type == Long.class) {
                    final var parsed = Long.parseLong(value);
                    yield i -> parsed;
                }
                if (type == double.class || type == Double.class) {
                    final var parsed = Double.parseDouble(value);
                    yield i -> parsed;
                }
                if (type == float.class || type == Float.class) {
                    final var parsed = Float.parseFloat(value);
                    yield i -> parsed;
                }
                if (type == boolean.class || type == Boolean.class) {
                    final var parsed = Boolean.parseBoolean(value);
                    yield i -> parsed;
                }
                throw new IllegalArgumentException("Unsupported type: " + type + " for " + property.value());
            }
        };
    }

    public static <A, B, C> Mapper<A, B, C> mapper(final Class<C> mapperDescriptor) {
        return new Mapper<>(mapperDescriptor);
    }

    private static class MappedMapper<A> implements Function<A, Object> {
        private final String name;
        private final Function<Object, Object> mapper;

        private volatile Field extractor;

        private MappedMapper(final String name, final Function<Object, Object> mapper) {
            this.name = name;
            this.mapper = mapper;
        }

        @Override
        public Object apply(final A a) {
            if (extractor == null) {
                try {
                    extractor = a.getClass().getDeclaredField(name);
                    if (!extractor.canAccess(a)) {
                        extractor.setAccessible(true);
                    }
                } catch (final NoSuchFieldException e) {
                    throw new IllegalArgumentException(e);
                }
            }
            try {
                return mapper.apply(extractor.get(a));
            } catch (final IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
