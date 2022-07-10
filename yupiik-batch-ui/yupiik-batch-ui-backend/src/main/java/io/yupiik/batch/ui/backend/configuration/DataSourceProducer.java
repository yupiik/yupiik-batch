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

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import org.apache.tomcat.jdbc.pool.DataSource;
import org.apache.tomcat.jdbc.pool.PoolProperties;

@ApplicationScoped
public class DataSourceProducer {
    @Produces
    public DataSource dataSource(final Configuration configuration) {
        final var properties = new PoolProperties();
        properties.setDriverClassName(configuration.getDriver());
        properties.setUrl(configuration.getUrl());
        properties.setUsername(configuration.getUsername());
        properties.setPassword(configuration.getPassword());
        properties.setTestOnBorrow(configuration.isTestOnBorrow());
        properties.setTestOnReturn(configuration.isTestOnReturn());
        properties.setTestWhileIdle(configuration.isTestWhileIdle());
        properties.setMinEvictableIdleTimeMillis(configuration.getMinEvictableIdleTime());
        properties.setTimeBetweenEvictionRunsMillis(configuration.getTimeBetweenEvictionRuns());
        properties.setValidationQuery(configuration.getValidationQuery());
        properties.setValidationQueryTimeout(configuration.getValidationQueryTimeout());
        properties.setDefaultAutoCommit(false);
        properties.setMinIdle(configuration.getMinIdle());
        properties.setMaxActive(configuration.getMaxActive());
        properties.setMaxIdle(configuration.getMaxActive());
        properties.setRemoveAbandoned(configuration.isRemoveAbandoned());
        properties.setRemoveAbandonedTimeout(configuration.getRemoveAbandonedTimeout());
        properties.setRollbackOnReturn(true);
        return new DataSource(properties);
    }

    public void release(@Disposes final DataSource dataSource) {
        dataSource.close();
    }
}
