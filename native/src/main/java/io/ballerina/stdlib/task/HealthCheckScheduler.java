/*
 * Copyright (c) 2025 WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.ballerina.stdlib.task;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static io.ballerina.stdlib.task.TokenAcquisition.getJdbcUrl;

/**
 * Scheduler for health check updates.
 */
public final class HealthCheckScheduler {
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private static final ExecutorService virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();

    public static final String HEALTH_CHECK_QUERY = "INSERT INTO health_check(task_id, group_id, last_heartbeat) " +
            "VALUES (?, ?, CURRENT_TIMESTAMP) ON CONFLICT (task_id, group_id) DO UPDATE " +
            "SET last_heartbeat = EXCLUDED.last_heartbeat";

    private HealthCheckScheduler() { }

    /**
     * Starts the health check updater to periodically update the heartbeat.
     *
     * @param dbConfig Database configuration
     * @param tokenId Token identifier
     * @param periodInSeconds Period between updates in seconds
     */
    public static void startHealthCheckUpdater(DatabaseConfig dbConfig, String tokenId,
                                               String groupId, int periodInSeconds) {
        Runnable task = () -> {
            Connection connection;
            try {
                String jdbcUrl = getJdbcUrl(dbConfig);
                connection = DriverManager.getConnection(jdbcUrl, dbConfig.user(), dbConfig.password());
            } catch (SQLException e) {
                throw new RuntimeException("Failed to rollback transaction", e);
            }
            try {
                connection.setAutoCommit(false);
                PreparedStatement stmt = connection.prepareStatement(HEALTH_CHECK_QUERY);
                stmt.setString(1, tokenId);
                stmt.setString(2, groupId);
                stmt.executeUpdate();
                connection.commit();
            } catch (SQLException e) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackException) {
                    throw new RuntimeException("Failed to rollback transaction", rollbackException);
                }
            } finally {
                closeConnection(connection);
            }
        };
        scheduler.scheduleAtFixedRate(
                () -> virtualThreadExecutor.submit(task),
                0, periodInSeconds, TimeUnit.SECONDS
        );
    }


    private static void closeConnection(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) { }
        }
    }

    public static void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
