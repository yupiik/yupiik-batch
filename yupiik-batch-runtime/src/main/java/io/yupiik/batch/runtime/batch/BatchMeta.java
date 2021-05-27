package io.yupiik.batch.runtime.batch;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target(TYPE)
@Retention(RUNTIME)
public @interface BatchMeta {
    /**
     * @return a short description about this batch and what it does.
     */
    String description();
}
