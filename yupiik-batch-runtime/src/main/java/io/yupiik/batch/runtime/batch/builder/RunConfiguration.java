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