/*
 * Copyright (c) 2021-2022 - Yupiik SAS - https://www.yupiik.com
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
package io.yupiik.batch.ui.backend.configuration;

import io.yupiik.uship.webserver.tomcat.TomcatWebServerConfiguration;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import org.apache.catalina.Context;
import org.apache.catalina.authenticator.BasicAuthenticator;
import org.apache.tomcat.util.descriptor.web.LoginConfig;
import org.apache.tomcat.util.descriptor.web.SecurityCollection;
import org.apache.tomcat.util.descriptor.web.SecurityConstraint;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;

@Dependent
public class TomcatConfigurationProducer {
    @Produces
    @ApplicationScoped
    public TomcatWebServerConfiguration configuration(final Configuration configuration) {
        final var tomcat = new TomcatWebServerConfiguration();
        tomcat.setPort(configuration.getPort());
        tomcat.setAccessLogPattern(configuration.getAccessLogPattern());
        if (configuration.getWebUsers() != null && !configuration.getWebUsers().isBlank()) {
            final var users = readUsers(configuration);
            tomcat.setTomcatCustomizers(List.of(t -> users.stringPropertyNames().forEach(user -> t.addUser(user, users.getProperty(user)))));
            tomcat.setContextCustomizers(List.of(this::enforceAuthentication));
        }
        return tomcat;
    }

    private void enforceAuthentication(final Context c) {
        c.getPipeline().addValve(new BasicAuthenticator());

        final var loginConfig = new LoginConfig();
        loginConfig.setAuthMethod("BASIC");
        loginConfig.setRealmName("Yupiik Batch UI");
        loginConfig.setCharset(StandardCharsets.UTF_8);
        c.setLoginConfig(loginConfig);

        final var securityCollection = new SecurityCollection();
        securityCollection.addPattern("/*");
        securityCollection.setName("Yupiik Batch UI Security");

        final var constraint = new SecurityConstraint();
        constraint.setCharset(StandardCharsets.UTF_8);
        constraint.setAuthConstraint(true);
        constraint.addCollection(securityCollection);
        constraint.addAuthRole("**");
        c.addConstraint(constraint);
    }

    private Properties readUsers(final Configuration configuration) {
        final var users = new Properties();
        try (final var reader = new StringReader(configuration.getWebUsers())) {
            users.load(reader);
        } catch (final IOException e) {
            throw new IllegalArgumentException(e);
        }
        return users;
    }
}
