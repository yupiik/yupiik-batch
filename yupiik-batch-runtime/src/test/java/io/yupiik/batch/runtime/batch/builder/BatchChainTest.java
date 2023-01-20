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
package io.yupiik.batch.runtime.batch.builder;

import io.yupiik.batch.runtime.batch.BatchPromise;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BatchChainTest {
    @Test
    void failOnTimeout() {
        final var configuration = new RunConfiguration();
        configuration.setFailOnTimeout(true);
        configuration.setMaxBatchPromiseAwait(1);

        final var infiniteStep = new BatchChain.BatchRoot<>() {
            @Override
            public Result<Object> execute() {
                return new Result<>(BatchPromise.of(null, new CompletableFuture<>()), Result.Type.CONTINUE);
            }

            @Override
            public String name() {
                return "test";
            }
        };
        assertInstanceOf(TimeoutException.class, assertThrows(IllegalStateException.class, () -> infiniteStep.run(configuration)).getCause());
    }
}
