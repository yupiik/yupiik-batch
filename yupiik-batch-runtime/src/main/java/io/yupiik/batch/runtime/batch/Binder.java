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
package io.yupiik.batch.runtime.batch;

import io.yupiik.batch.runtime.configuration.Param;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.List;

import static java.util.Optional.ofNullable;

/**
 * @see {@link io.yupiik.batch.runtime.configuration.Binder} too.
 * This one only maintain the compatibility with the old {@link io.yupiik.batch.runtime.batch.Param}.
 */
public class Binder extends io.yupiik.batch.runtime.configuration.Binder {
    public Binder(final String prefix, final List<String> args) {
        super(prefix, args);
    }

    @Override
    protected Binder newNestedBinder(final String paramName, final List<String> args) {
        return new Binder(paramName, args) {
            @Override // enables to keep the overridden behavior when the root binder is a child
            protected void doBind(final Object instance, final Field param, final Param conf, final String paramName) {
                Binder.this.doBind(instance, param, conf, paramName);
            }
        };
    }

    @Override
    protected boolean isParam(final Field field) {
        return super.isParam(field) || field.isAnnotationPresent(io.yupiik.batch.runtime.batch.Param.class);
    }

    @Override
    protected Param getParam(final Field field) {
        return ofNullable(super.getParam(field))
                .or(() -> ofNullable(field.getAnnotation(io.yupiik.batch.runtime.batch.Param.class))
                        .map(this::toNewParam))
                .orElse(null);
    }

    private Param toNewParam(final io.yupiik.batch.runtime.batch.Param param) {
        return Param.class.cast(Proxy.newProxyInstance(
                Thread.currentThread().getContextClassLoader(), new Class<?>[]{Param.class},
                (proxy, method, args) -> {
                    try {
                        return io.yupiik.batch.runtime.batch.Param.class
                                .getMethod(method.getName(), method.getParameterTypes())
                                .invoke(param, args);
                    } catch (final InvocationTargetException ite) {
                        throw ite.getTargetException();
                    }
                }));
    }
}
