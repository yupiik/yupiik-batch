package io.yupiik.batch.runtime.sql;

import io.yupiik.batch.runtime.batch.Param;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;
import java.util.StringJoiner;

public class DataSourceConfiguration {
    @Param(description = "Driver to use", required = true)
    private String driver;

    @Param(description = "JDBC URL to use", required = true)
    private String url;

    @Param(description = "Database username.")
    private String username;

    @Param(description = "Database password.")
    private String password;

    // no need of pool for the batches normally
    public SQLSupplier<Connection> toConnectionProvider() {
        final var properties = new Properties();
        if (username != null) {
            properties.setProperty("user", username);
        }
        if (password != null) {
            properties.setProperty("password", username);
        }
        return () -> DriverManager.getConnection(url, properties);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", DataSourceConfiguration.class.getSimpleName() + "[", "]")
                .add("driver='" + driver + "'")
                .add("url='" + url + "'")
                .add("username='" + username + "'")
                .add("password='" + password + "'")
                .toString();
    }
}
