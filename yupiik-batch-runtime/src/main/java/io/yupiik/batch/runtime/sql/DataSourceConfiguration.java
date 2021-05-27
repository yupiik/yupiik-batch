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
