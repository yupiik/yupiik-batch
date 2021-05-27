package io.yupiik.batch.runtime.component.mapping;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Enables to define a mapping between an input record and output record.
 */
@Target(TYPE)
@Retention(RUNTIME)
public @interface Mapping {
    /**
     * @return expected input type.
     */
    Class<?> from() default Object.class;

    /**
     * @return expected output type.
     */
    Class<? extends Record> to();

    /**
     * IMPORTANT: no implicit mapping, can lead to errors too easily.
     *
     * @return how properties are mapped from the input to the output.
     */
    Property[] properties() default {};

    /**
     * @return list of defined mapping tables.
     */
    MappingTable[] tables() default {};

    /**
     * @return a short description of this mapper.
     */
    String documentation() default "";

    /**
     * @return true to not include this mapper in the documentation.
     */
    boolean enableInDocumentation() default true;

    @Target(PARAMETER)
    @Retention(RUNTIME)
    @interface Table {
        /**
         * Enables to inject in a custom mapper the map of string/string from the annotation.
         *
         * @return a mapping table name.
         */
        String value();
    }

    @Target(TYPE)
    @Retention(RUNTIME)
    @interface MappingTable {
        String name();

        Entry[] entries();
    }

    @Target(TYPE)
    @Retention(RUNTIME)
    @interface Entry {
        String input();

        String output();
    }

    @Target(TYPE)
    @Retention(RUNTIME)
    @interface Property {
        PropertyType type() default PropertyType.MAPPED;

        String from() default "";

        String to();

        String value() default "";

        OnTableMappingLookupFailure onMissedTableLookup() default OnTableMappingLookupFailure.FAIL;
    }

    enum OnTableMappingLookupFailure {
        /**
         * Set null.
         */
        NULL,
        /**
         * Keep incoming value.
         */
        FORWARD,
        /**
         * Throw an exception.
         */
        FAIL
    }

    /**
     * Enables to define a custom mapping.
     */
    @Target(METHOD)
    @Retention(RUNTIME)
    public @interface Custom {
        /**
         * Name of the target attribute.
         * If empty, name will match the method name.
         *
         * @return the name of the target attribute.
         */
        String to() default "";

        /**
         * Explains the mapping rule implemented in java.
         *
         * @return the description of this field mapping.
         */
        String description();
    }

    enum PropertyType {
        /**
         * Will copy {@code from} property value from the input to {@code to} property in the output.
         */
        MAPPED {
            @Override
            public void doValidate(final Property property) {
                if (property.from().isEmpty()) {
                    throw new IllegalArgumentException("You can't use @Property(type=MAPPED) without specifying from");
                }
            }
        },

        /**
         * Will set {@code value} property to {@code to} property in the output.
         */
        CONSTANT {
            @Override
            public void doValidate(final Property property) {
                if (property.value().isEmpty()) {
                    throw new IllegalArgumentException("You can't use @Property(type=CONSTANT) without specifying value");
                }
            }
        },

        /**
         * Will lookup the output ({@code to}) value in the table mapping reference by {@code value} using as key the string value of the {@code from} parameter.
         * If {@code failIfMissingEntry} is true, it throws an exception when the mapping is incomplete.
         */
        TABLE_MAPPING {
            @Override
            public void doValidate(final Property property) {
                if (property.value().isEmpty()) {
                    throw new IllegalArgumentException("You can't use @Property(type=TABLE_MAPPING) without specifying value");
                }
            }
        };

        protected abstract void doValidate(Property property);

        public void validate(final Property property) {
            doValidate(property);
        }
    }
}
