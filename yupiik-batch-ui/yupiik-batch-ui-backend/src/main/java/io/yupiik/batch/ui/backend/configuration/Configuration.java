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
package io.yupiik.batch.ui.backend.configuration;

import io.yupiik.batch.runtime.batch.Binder;
import io.yupiik.batch.runtime.batch.Param;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

@ApplicationScoped
public class Configuration {
    @Param(name = "yupiik.batch.frontend.extensionsJs", description = "" +
            "Javascript path to a Yupiik Batch frontend extension. " +
            "Script is injected just before yupiik-batch main one, i.e. after libraries ones but note they are minified wih webpack.")
    private String frontendExtensionsJs;

    @Param(name = "yupiik.batch.backend.tomcat.port", description = "Tomcat port.")
    private int port = 8080;

    @Param(name = "yupiik.batch.backend.tomcat.accessLogPattern", description = "Tomcat access log pattern.")
    private String accessLogPattern = "common";

    @Param(name = "yupiik.batch.backend.tomcat.webUsers", description = "" +
            "List of allowed users - by default all are. It uses a properties syntax: `user=password`. " +
            "Security uses a web BASIC mecanism.")
    private String webUsers;

    @Param(name = "yupiik.batch.backend.datasource.driver", required = true, description = "Datasource driver.")
    private String driver;

    @Param(name = "yupiik.batch.backend.datasource.url", required = true, description = "Datasource URL.")
    private String url;

    @Param(name = "yupiik.batch.backend.datasource.username", description = "Datasource username.")
    private String username;

    @Param(name = "yupiik.batch.backend.datasource.password", description = "Datasource password.")
    private String password;

    @Param(name = "yupiik.batch.backend.datasource.testOnBorrow", description = "Should connections be tested on borrow time.")
    private boolean testOnBorrow;

    @Param(name = "yupiik.batch.backend.datasource.testOnReturn", description = "Should connections be tested on return to the pool time.")
    private boolean testOnReturn;

    @Param(name = "yupiik.batch.backend.datasource.testWhileIdle", description = "Should connections be tested in background.")
    private boolean testWhileIdle;

    @Param(name = "yupiik.batch.backend.datasource.timeBetweenEvictionRuns", description = "Time between background evictions in ms.")
    private int timeBetweenEvictionRuns;

    @Param(name = "yupiik.batch.backend.datasource.minEvictableIdleTime", description = "How long to await before a connection is considered idled and evictable.")
    private int minEvictableIdleTime;

    @Param(name = "yupiik.batch.backend.datasource.validationQuery", description = "Validation query to validate the connection when enabled.")
    private String validationQuery;

    @Param(name = "yupiik.batch.backend.datasource.validationQueryTimeout", description = "How long to await for the validation query.")
    private int validationQueryTimeout;

    @Param(name = "yupiik.batch.backend.datasource.minIdle", description = "Min connections in the pool.")
    private int minIdle = 2;

    @Param(name = "yupiik.batch.backend.datasource.maxConnections", description = "Max connections in the pool.")
    private int maxActive = 16;

    @Param(name = "yupiik.batch.backend.database.jobTable", description = "Job table to query.")
    private String jobTable = "BATCH_JOB_EXECUTION_TRACE";

    @Param(name = "yupiik.batch.backend.database.stepTable", description = "Step table to query.")
    private String stepTable = "BATCH_STEP_EXECUTION_TRACE";

    @Param(name = "yupiik.batch.backend.queries.findAllJobs",
            description = "Find all jobs with pagination SQL query (for portability), `${table}` is replaced by the table name. " +
                    "Parameters can be `${pageSize}`, `${firstIndex}` - inclusive, `${lastIndex}` - exclusive.")
    private String findAllJobs = "" +
            "SELECT id, name, status, comment, started, finished " +
            "FROM ${table} " +
            "ORDER BY finished DESC " +
            "LIMIT ${pageSize} OFFSET ${firstIndex}";

    @Param(name = "yupiik.batch.backend.queries.countAllJobs",
            description = "Count all jobs SQL query (for portability), `${table}` is replaced by the table name.")
    private String countAllJobs = "SELECT count(*) FROM ${table}";

    @Param(name = "yupiik.batch.backend.queries.findJobById",
            description = "Find a job by id SQL query (for portability), `${table}` is replaced by the table name.")
    private String findJobById = "SELECT id, name, status, comment, started, finished FROM ${table} WHERE id = ?";

    @Param(name = "yupiik.batch.backend.queries.findStepsByJobId",
            description = "Find aall steps related to a job id SQL query (for portability), `${table}` is replaced by the step table name.")
    private String findStepsByJobId = "SELECT id, name, status, comment, started, finished, previous_id FROM ${table} WHERE job_id = ?";

    @PostConstruct
    private void init() {
        final var params = System.getProperties().stringPropertyNames().stream()
                .filter(it -> it.startsWith("yupiik."))
                .flatMap(it -> Stream.of("--" + it, System.getProperty(it)))
                .collect(toList());
        new Binder(null, params).bind(this);
    }

    public String getFrontendExtensionsJs() {
        return frontendExtensionsJs;
    }

    public String getWebUsers() {
        return webUsers;
    }

    public int getPort() {
        return port;
    }

    public String getAccessLogPattern() {
        return accessLogPattern;
    }

    public int getMinIdle() {
        return minIdle;
    }

    public int getMaxActive() {
        return maxActive;
    }

    public String getDriver() {
        return driver;
    }

    public String getFindJobById() {
        return findJobById;
    }

    public String getFindStepsByJobId() {
        return findStepsByJobId;
    }

    public String getFindAllJobs() {
        return findAllJobs;
    }

    public String getCountAllJobs() {
        return countAllJobs;
    }

    public String getJobTable() {
        return jobTable;
    }

    public String getStepTable() {
        return stepTable;
    }

    public String getUrl() {
        return url;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public boolean isTestOnBorrow() {
        return testOnBorrow;
    }

    public boolean isTestOnReturn() {
        return testOnReturn;
    }

    public boolean isTestWhileIdle() {
        return testWhileIdle;
    }

    public int getTimeBetweenEvictionRuns() {
        return timeBetweenEvictionRuns;
    }

    public int getMinEvictableIdleTime() {
        return minEvictableIdleTime;
    }

    public String getValidationQuery() {
        return validationQuery;
    }

    public int getValidationQueryTimeout() {
        return validationQueryTimeout;
    }
}
