package io.yupiik.batch.runtime.batch;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Mark a primitive, {@code String} or {@code List<String>} to be read from the main args.
 * When a list, the parameter can be repeated to get multiple values.
 *
 * Note that if the command line does not have the parameter and it is not a list, it is also read from the environment variables.
 * (normalized,  {@code foo.bar} becomes {@code FOO_BAR} for example).
 */
@Target(FIELD)
@Retention(RUNTIME)
public @interface Param {
    /**
     * The name of the parameter in the CLI without leading {@code -}.
     * Will match with one or two leading {@code -} and will support {@code --no-} prefix (value set to false).
     *
     * If no value follows the argument, value is set to {@code false}.
     *
     * If the value is not set, the field name is used.
     *
     * @return name of the parameter.
     */
    String name() default "";

    /**
     * For a list, required means not empty.
     *
     * @return true if the binding should fail if parameter is required.
     */
    boolean required() default false;

    /**
     * @return some light description on what the parameter does.
     */
    String description();
}
