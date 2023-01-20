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
package io.yupiik.batch.ui.backend.sql;

import io.yupiik.batch.runtime.sql.SQLFunction;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class IteratingResultset<A> implements Iterator<A> {
    private final SQLFunction<ResultSet, A> mapper;
    private final ResultSet resultSet;

    public IteratingResultset(final SQLFunction<ResultSet, A> mapper, final ResultSet resultSet) {
        this.mapper = mapper;
        this.resultSet = resultSet;
    }

    @Override
    public boolean hasNext() {
        try {
            return resultSet.next();
        } catch (final SQLException throwables) {
            throw new IllegalStateException(throwables);
        }
    }

    @Override
    public A next() {
        try {
            return mapper.apply(resultSet);
        } catch (final SQLException throwables) {
            throw new IllegalStateException(throwables);
        }
    }

    public static <A> List<A> toList(final ResultSet resultSet, final SQLFunction<ResultSet, A> mapper) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(
                new IteratingResultset<>(mapper, resultSet), Spliterator.IMMUTABLE), false)
                .collect(Collectors.toList());
    }
}
