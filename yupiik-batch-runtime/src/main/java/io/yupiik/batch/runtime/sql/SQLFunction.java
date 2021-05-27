package io.yupiik.batch.runtime.sql;

import java.sql.SQLException;

@FunctionalInterface
public interface SQLFunction<A, B> {
    B apply(A param) throws SQLException;
}