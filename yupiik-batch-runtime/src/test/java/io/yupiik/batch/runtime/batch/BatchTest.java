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
package io.yupiik.batch.runtime.batch;

import io.yupiik.batch.runtime.batch.builder.Executable;
import io.yupiik.batch.runtime.batch.builder.RunConfiguration;
import io.yupiik.batch.runtime.fn.CommentifiableConsumer;
import io.yupiik.batch.runtime.fn.CommentifiableFunction;
import io.yupiik.batch.runtime.fn.CommentifiablePredicate;
import io.yupiik.batch.runtime.tracing.BaseExecutionTracer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class BatchTest {
    @AfterEach
    void reset() {
        MyBatch.last = null;
    }

    @Test
    void run() {
        Batch.run(MyBatch.class);
        assertNotNull(MyBatch.last);
        assertNull(MyBatch.last.foo);

        Batch.run(MyBatch.class, "--ignored");
        assertNotNull(MyBatch.last);
        assertNull(MyBatch.last.foo);

        Batch.run(MyBatch.class, "--ignored2", "yes");
        assertNotNull(MyBatch.last);
        assertNull(MyBatch.last.foo);

        Batch.run(MyBatch.class, "--mybatch-foo", "bar");
        assertNotNull(MyBatch.last);
        assertEquals("bar", MyBatch.last.foo);
    }

    @Test
    void commentable() {
        final var result = new AtomicReference<Map.Entry<BaseExecutionTracer.JobExecution, List<BaseExecutionTracer.StepExecution>>>();
        final var tracer = new BaseExecutionTracer("test", Clock.systemUTC()) {

            @Override
            protected void save(JobExecution execution, List<StepExecution> steps) {
                result.set(Map.entry(execution, steps));
            }
        };
        final var runConfiguration = new RunConfiguration();
        runConfiguration.setExecutionWrapper(tracer::traceExecution);
        runConfiguration.setElementExecutionWrapper(e -> (c, r) -> Executable.Result.class.cast(tracer.traceStep(c, e, r)));

        final var conf = new Conf();
        conf.foo = "too";

        new Batch<Conf>() {
            @Override
            public void accept(final Conf conf) {
                final var root = from();
                final var map = root
                        .map("map1", new CommentifiableFunction<Void, String>() {
                            @Override
                            public String apply(final Void unused) {
                                return conf.foo;
                            }

                            @Override
                            public String toComment() {
                                return conf.foo + " map step";
                            }
                        });
                final var filter = map
                        .filter("filter1", new CommentifiablePredicate<>() {
                            @Override
                            public boolean test(final String t) {
                                return t.startsWith("to");
                            }

                            @Override
                            public String toComment() {
                                return conf.foo + " filter step";
                            }
                        });
                final var then = filter
                        .then("then1", new CommentifiableConsumer<>() {
                            @Override
                            public void accept(final String s) {
                                // no-op
                            }

                            @Override
                            public String toComment() {
                                return conf.foo + " consume step";
                            }
                        });
                then.run(runConfiguration);
            }
        }.accept(conf);

        final var executions = result.get();
        assertNotNull(executions);
        assertEquals(
                List.of("too map step", "too filter step", "too consume step"),
                executions.getValue().stream().map(BaseExecutionTracer.StepExecution::comment).collect(toList()));
    }

    @Test
    void dsl() {
        final var conf = new Conf();
        conf.foo = "toto";
        final var spy = new ArrayList<String>();
        new Batch<Conf>() {
            @Override
            public void accept(final Conf conf) {
                from()
                        .map("map1", n -> {
                            spy.add("visit1: " + n);
                            return conf.foo;
                        })
                        .filter("filter1", t -> {
                            spy.add("filter1: " + t);
                            return t.startsWith("to");
                        })
                        .then("then1", it -> spy.add("peek1: " + it))
                        .then("then2", it -> spy.add("peek2: " + it))
                        .map("map2", n -> {
                            spy.add("visit2: " + n);
                            return new StringBuilder(conf.foo).reverse().toString();
                        })
                        .filter("filter2", t -> {
                            spy.add("filter2: " + t);
                            return t.startsWith("o");
                        })
                        .run(null);
            }
        }.accept(conf);
        assertEquals(
                List.of("visit1: null", "filter1: toto", "peek1: toto", "peek2: toto", "visit2: toto", "filter2: otot"),
                spy);

        spy.clear();
        new Batch<Conf>() {
            @Override
            public void accept(final Conf conf) {
                from()
                        .map("map1", n -> {
                            spy.add("visit1: " + n);
                            return conf.foo;
                        })
                        .filter("filter1", t -> {
                            spy.add("filter1: " + t);
                            return t.startsWith("o");
                        })
                        .then("then1", it -> spy.add("peek1: " + it))
                        .then("then2", it -> spy.add("peek2: " + it))
                        .map("map2", n -> {
                            spy.add("visit2: " + n);
                            return new StringBuilder(conf.foo).reverse().toString();
                        })
                        .run(null);
            }
        }.accept(conf);
        assertEquals(List.of("visit1: null", "filter1: toto"), spy);
    }

    public static class Conf {
        @Param(description = "")
        private String foo;
    }

    @BatchMeta(description = "")
    public static class MyBatch implements Batch<Conf> {
        private static Conf last;

        @Override
        public void accept(final Conf conf) {
            last = conf;
        }
    }
}
