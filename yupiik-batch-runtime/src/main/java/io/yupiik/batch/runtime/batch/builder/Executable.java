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
package io.yupiik.batch.runtime.batch.builder;

public interface Executable<P, R> {
    /**
     * Operation implementation.
     * Important: it is unlikely to be used in user code where {@code run} is preferred, only in DSL components.
     *
     * @return the result of this operation.
     */
    // @Protected
    Result<R> execute(RunConfiguration configuration, Result<P> previous);

    record Result<T>(T value, Type type) {
        public enum Type {
            // chain is stopped
            SKIP,
            // chain continues in the same thread
            CONTINUE
        }
    }
}
