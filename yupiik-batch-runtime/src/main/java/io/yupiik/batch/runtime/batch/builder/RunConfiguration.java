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
package io.yupiik.batch.runtime.batch.builder;

import java.util.function.Function;

public class RunConfiguration { // don't use a record, we don't want to break batches cause we added a toggle/config
    Function<Runnable, Runnable> executionWrapper;
    Function<BatchChain<?, ?, ?>, Executable<?, ?>> elementExecutionWrapper;

    public RunConfiguration setExecutionWrapper(final Function<Runnable, Runnable> executionWrapper) {
        this.executionWrapper = executionWrapper;
        return this;
    }

    public RunConfiguration setElementExecutionWrapper(final Function<BatchChain<?, ?, ?>, Executable<?, ?>> elementExecutionWrapper) {
        this.elementExecutionWrapper = elementExecutionWrapper;
        return this;
    }
}