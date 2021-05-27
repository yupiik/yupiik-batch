package io.yupiik.batch.runtime.sql;

import java.sql.SQLException;

@FunctionalInterface
public interface SQLSupplier<A> {
    A get() throws SQLException;
}