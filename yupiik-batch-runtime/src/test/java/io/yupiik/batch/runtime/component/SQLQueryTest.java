package io.yupiik.batch.runtime.component;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SQLQueryTest {
    @Test
    void normal() throws SQLException {
        final var dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:SQLQueryTest_normal");
        try (final var keepDb = dataSource.getConnection()) { // avoid h2 to delete the table with the last close()
            try (final var connection = dataSource.getConnection();
                 final var statement = connection.createStatement()) {
                statement.execute("" +
                        "CREATE TABLE SQLQueryTest_normal (" +
                        "   id INT NOT NULL," +
                        "   name VARCHAR(50) NOT NULL" +
                        ")");
                statement.execute("" +
                        "INSERT INTO SQLQueryTest_normal" +
                        " (id, name) VALUES " +
                        " (1, 'romain')");
                statement.execute("" +
                        "INSERT INTO SQLQueryTest_normal" +
                        " (id, name) VALUES " +
                        " (2, 'francois')");
                connection.commit();
            }

            final var publisher = new SQLQuery<>(
                    dataSource::getConnection, "select id, name from SQLQueryTest_normal order by id",
                    rs -> Map.of(rs.getInt("id"), rs.getString("name")));
            assertEquals(List.of(
                    Map.of(1, "romain"),
                    Map.of(2, "francois")
            ), StreamSupport.stream(Spliterators.spliteratorUnknownSize(publisher, Spliterator.IMMUTABLE), false).collect(toList()));
        }
    }
}
