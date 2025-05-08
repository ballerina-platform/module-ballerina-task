/*
 *  Copyright (c) 2025 WSO2 LLC. (http://www.wso2.com).
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package io.ballerina.stdlib.task.coordination;

import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.utils.TypeUtils;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.stdlib.task.utils.Utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;

import static io.ballerina.stdlib.task.server.TaskServerJob.handleRollback;

/**
 * Handles token acquisition with proper transaction management.
 */
public final class TokenAcquisition {
    public static final BString DB_HOST = StringUtils.fromString("host");
    public static final BString DB_USER = StringUtils.fromString("user");
    public static final BString DB_PASSWORD = StringUtils.fromString("password");
    public static final BString DB_PORT = StringUtils.fromString("port");
    public static final BString DATABASE = StringUtils.fromString("database");
    public static final BString DB_TYPE = StringUtils.fromString("dbType");
    public static final BString DATABASE_CONFIG = StringUtils.fromString("databaseConfig");
    public static final BString STATUS = StringUtils.fromString("status");
    public static final BString ACTIVE_STATUS = StringUtils.fromString("active");
    public static final BString STANDBY_STATUS = StringUtils.fromString("standby");
    public static final BString TASK_ID = StringUtils.fromString("taskId");
    public static final BString GROUP_ID = StringUtils.fromString("groupId");
    public static final BString TOKEN_HOLDER = StringUtils.fromString("tokenholder");
    public static final BString LIVENESS_INTERVAL = StringUtils.fromString("livenessInterval");
    public static final String LAST_HEARTBEAT = "last_heartbeat";
    public static final String ID = "task_id";

    public static final String DB_TYPE_POSTGRESQL = "postgresql";
    public static final String DB_TYPE_MYSQL = "mysql";
    public static final String POSTGRESQL_JDBC_URL = "jdbc:postgresql://%s:%d/%s";
    public static final String MYSQL_JDBC_URL = "jdbc:mysql://%s:%d/%s";
    public static final String HAS_ACTIVE_TOKEN_QUERY = "SELECT task_id FROM token_holder " +
            "WHERE task_id = ? AND group_id = ?";
    public static final String GET_CURRENT_TOKEN_QUERY = "SELECT task_id FROM token_holder WHERE group_id = ?";
    public static final String CURRENT_TIMESTAMP_QUERY = "SELECT CURRENT_TIMESTAMP";
    public static final String HEALTH_CHECK_QUERY = "SELECT last_heartbeat FROM health_check WHERE task_id = ? " +
            "AND group_id = ? ORDER BY last_heartbeat DESC LIMIT 1";

    public static final String POSTGRESQL_UPSERT_TOKEN_QUERY =
            "INSERT INTO token_holder(task_id, group_id, term) VALUES (?, ?, 1) " +
            "ON CONFLICT (group_id) DO UPDATE SET task_id = EXCLUDED.task_id, term = token_holder.term + 1";
    public static final String MYSQL_UPSERT_TOKEN_QUERY =
            "INSERT INTO token_holder(task_id, group_id, term) VALUES (?, ?, 1) " +
            "ON DUPLICATE KEY UPDATE task_id = VALUES(task_id), term = term + 1";

    public static final String POSTGRESQL_CONFIG = "PostgresqlConfig";

    private TokenAcquisition() { }

    /**
     * Acquires token with proper transaction handling.
     */
    public static Object acquireToken(BMap<Object, Object> databaseConfig,
                                      BString id, BString groupId, boolean tokenAcquired, int livenessInterval,
                                      int heartbeatFrequency) throws SQLException {
        String dbType = TypeUtils.getType(databaseConfig).getName().contains(POSTGRESQL_CONFIG)
                ? DB_TYPE_POSTGRESQL : DB_TYPE_MYSQL;
        if (databaseConfig.containsKey(DB_TYPE)) {
            dbType = databaseConfig.getStringValue(DB_TYPE).getValue().toLowerCase();
        }
        DatabaseConfig dbConfig = new DatabaseConfig(
                databaseConfig.getStringValue(DB_HOST).getValue(), databaseConfig.getStringValue(DB_USER).getValue(),
                databaseConfig.getStringValue(DB_PASSWORD).getValue(), databaseConfig.getIntValue(DB_PORT).intValue(),
                databaseConfig.getStringValue(DATABASE).getValue(), dbType
        );
        Connection connection = null;
        try {
            String instanceId = id.getValue();
            String jdbcUrl = getJdbcUrl(dbConfig);
            connection = DriverManager.getConnection(jdbcUrl, dbConfig.user(), dbConfig.password());
            connection.setAutoCommit(false);
            tokenAcquired = attemptTokenAcquisition(connection, instanceId, groupId.getValue(),
                    tokenAcquired, livenessInterval, dbType);
            connection.commit();
            HealthCheckScheduler.startHealthCheckUpdater(dbConfig, instanceId, groupId.getValue(), heartbeatFrequency);
            return generateResponse(tokenAcquired, livenessInterval, instanceId, groupId.getValue(), dbConfig);
        } catch (Exception e) {
            handleRollback(connection);
            throw Utils.createTaskError(e.getMessage());
        }
    }

    public static String getJdbcUrl(DatabaseConfig dbConfig) {
        String dbType = dbConfig.dbType();
        if (DB_TYPE_MYSQL.equals(dbType)) {
            return String.format(MYSQL_JDBC_URL, dbConfig.host(), dbConfig.port(), dbConfig.database());
        } else {
            return String.format(POSTGRESQL_JDBC_URL, dbConfig.host(), dbConfig.port(), dbConfig.database());
        }
    }

    private static BMap<BString, Object> generateResponse(boolean tokenAcquired, int interval, String instanceId,
                                                          String groupId, DatabaseConfig databaseConfig) {
        BMap<BString, Object> response = ValueCreator.createMapValue();
        response.put(STATUS, tokenAcquired ? ACTIVE_STATUS : STANDBY_STATUS);
        response.put(TASK_ID, StringUtils.fromString(instanceId));
        response.put(GROUP_ID, StringUtils.fromString(groupId));
        response.put(TOKEN_HOLDER, tokenAcquired);
        response.put(DATABASE_CONFIG, databaseConfig);
        response.put(LIVENESS_INTERVAL, interval);
        return response;
    }

    public static boolean hasActiveToken(Connection connection, String taskId, String groupId) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(HAS_ACTIVE_TOKEN_QUERY)) {
            stmt.setString(1, taskId);
            stmt.setString(2, groupId);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        }
    }

    public static boolean attemptTokenAcquisition(Connection connection, String taskId, String groupId,
                                                  boolean hasToken, int livenessInterval, String dbType)
            throws SQLException {
        boolean acquiredToken = hasToken;
        try (PreparedStatement stmt = connection.prepareStatement(GET_CURRENT_TOKEN_QUERY)) {
            stmt.setString(1, groupId);
            ResultSet result = stmt.executeQuery();
            if (!result.next()) {
                String upsertQuery = DB_TYPE_MYSQL.equals(dbType)
                        ? MYSQL_UPSERT_TOKEN_QUERY : POSTGRESQL_UPSERT_TOKEN_QUERY;
                PreparedStatement upsertStmt = connection.prepareStatement(upsertQuery);
                upsertStmt.setString(1, taskId);
                upsertStmt.setString(2, groupId);
                upsertStmt.executeUpdate();
                return true;
            }

            String existingTokenId = result.getString(ID);
            if (existingTokenId.equals(taskId)) {
                return true;
            }

            try (PreparedStatement currentTimeStmt = connection.prepareStatement(CURRENT_TIMESTAMP_QUERY)) {
                ResultSet timeResult = currentTimeStmt.executeQuery();
                Timestamp currentTime = timeResult.next()
                        ? timeResult.getTimestamp(1) : Timestamp.from(Instant.now());

                PreparedStatement healthStmt = connection.prepareStatement(HEALTH_CHECK_QUERY);
                healthStmt.setString(1, existingTokenId);
                healthStmt.setString(2, groupId);
                ResultSet healthCheckRs = healthStmt.executeQuery();

                if (healthCheckRs.next()) {
                    Timestamp lastHeartbeat = healthCheckRs.getTimestamp(LAST_HEARTBEAT);
                    long timeDiffSeconds = (currentTime.getTime() - lastHeartbeat.getTime()) / 1000;

                    if (timeDiffSeconds > livenessInterval) {
                        String upsertQuery = DB_TYPE_MYSQL.equals(dbType)
                                ? MYSQL_UPSERT_TOKEN_QUERY : POSTGRESQL_UPSERT_TOKEN_QUERY;
                        PreparedStatement upsertStmt = connection.prepareStatement(upsertQuery);
                        upsertStmt.setString(1, taskId);
                        upsertStmt.setString(2, groupId);
                        upsertStmt.executeUpdate();
                        acquiredToken = true;
                    }
                } else {
                    String upsertQuery = DB_TYPE_MYSQL.equals(dbType)
                            ? MYSQL_UPSERT_TOKEN_QUERY : POSTGRESQL_UPSERT_TOKEN_QUERY;
                    PreparedStatement upsertStmt = connection.prepareStatement(upsertQuery);
                    upsertStmt.setString(1, taskId);
                    upsertStmt.setString(2, groupId);
                    upsertStmt.executeUpdate();
                    acquiredToken = true;
                }
            }
        }
        return acquiredToken;
    }
}
