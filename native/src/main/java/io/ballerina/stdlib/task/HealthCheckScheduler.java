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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static io.ballerina.stdlib.task.TokenAcquisition.JDBC_URL;

/**
 * Scheduler for health check updates.
 */
public class HealthCheckScheduler {
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    public static final String HEALTH_CHECK_QUERY = "INSERT INTO health_check(token_id, last_heartbeat) " +
            "VALUES (?, CURRENT_TIMESTAMP) ON DUPLICATE KEY UPDATE last_heartbeat = CURRENT_TIMESTAMP";

    private HealthCheckScheduler() {
        // Private constructor to prevent instantiation
    }

    /**
     * Dedicated exception for database transaction failures.
     */
    public static class DatabaseTransactionException extends SQLException {
        public DatabaseTransactionException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Starts the health check updater to periodically update the heartbeat.
     *
     * @param dbConfig Database configuration
     * @param tokenId Token identifier
     * @param periodInSeconds Period between updates in seconds
     */
    public static void startHealthCheckUpdater(DatabaseConfig dbConfig, String tokenId, int periodInSeconds) {
        Runnable task = () -> {
            boolean needsRollback = false;
            Connection connection = null;

            try {
                String jdbcUrl = String.format(JDBC_URL, dbConfig.host(), dbConfig.port(), dbConfig.database());
                connection = DriverManager.getConnection(jdbcUrl, dbConfig.user(), dbConfig.password());
                connection.setAutoCommit(false);
                needsRollback = true;

                try (PreparedStatement stmt = connection.prepareStatement(HEALTH_CHECK_QUERY)) {
                    stmt.setString(1, tokenId);
                    stmt.executeUpdate();
                    connection.commit();
                    needsRollback = false;
                }
            } catch (SQLException e) {
                if (connection != null && needsRollback) {
                    try {
                        connection.rollback();
                    } catch (SQLException rollbackException) {
                        throw new RuntimeException("Failed to rollback transaction", rollbackException);
                    }
                }
            } finally {
                closeConnection(connection);
            }
        };

        scheduler.scheduleAtFixedRate(task, 0, periodInSeconds, TimeUnit.SECONDS);
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
