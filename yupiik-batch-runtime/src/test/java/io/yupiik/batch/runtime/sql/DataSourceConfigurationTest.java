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

import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DataSourceConfigurationTest {
    @Test
    void credentials() throws Exception {
        final var configuration = new DataSourceConfiguration();
        final var username = DataSourceConfiguration.class.getDeclaredField("username");
        final var password = DataSourceConfiguration.class.getDeclaredField("password");
        final var url = DataSourceConfiguration.class.getDeclaredField("url");
        Stream.of(username, password, url).forEach(f -> f.setAccessible(true));
        username.set(configuration, "test1");
        password.set(configuration, "test2");
        url.set(configuration, "jdbc:test");
        final var props = new AtomicReference<Properties>();
        final var driver = new Driver() {
            @Override
            public Connection connect(final String url, final Properties info) {
                props.set(info);
                return Connection.class.cast(Proxy.newProxyInstance(
                        Thread.currentThread().getContextClassLoader(),
                        new Class<?>[]{Connection.class},
                        (proxy, method, args) -> null));
            }

            @Override
            public boolean acceptsURL(final String url) {
                return "jdbc:test".equals(url);
            }

            @Override
            public DriverPropertyInfo[] getPropertyInfo(final String url, final Properties info) {
                return new DriverPropertyInfo[0];
            }

            @Override
            public int getMajorVersion() {
                return 4;
            }

            @Override
            public int getMinorVersion() {
                return 1;
            }

            @Override
            public boolean jdbcCompliant() {
                return false;
            }

            @Override
            public Logger getParentLogger() {
                return null;
            }
        };
        DriverManager.registerDriver(driver);
        configuration.toConnectionProvider().get();

        final var properties = props.get();
        DriverManager.deregisterDriver(driver);

        assertNotNull(properties);
        assertEquals("test1", properties.getProperty("user"));
        assertEquals("test2", properties.getProperty("password"));
    }
}
