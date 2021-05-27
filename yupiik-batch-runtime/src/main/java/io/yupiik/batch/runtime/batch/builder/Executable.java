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
