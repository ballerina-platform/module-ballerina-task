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

package io.ballerina.stdlib.task;

import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.utils.StringUtils;
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

import static io.ballerina.stdlib.task.utils.Utils.handleRollback;

/**
 * Handles token acquisition with proper transaction management.
 */
public final class TokenAcquisition {
    public static final BString DB_HOST = StringUtils.fromString("host");
    public static final BString DB_USER = StringUtils.fromString("user");
    public static final BString DB_PASSWORD = StringUtils.fromString("password");
    public static final BString DB_PORT = StringUtils.fromString("port");
    public static final BString DATABASE = StringUtils.fromString("database");
    public static final BString DATABASE_CONFIG = StringUtils.fromString("databaseConfig");
    public static final String JDBC_URL = "jdbc:mysql://%s:%d/%s";
    public static final BString STATUS = StringUtils.fromString("status");
    public static final BString ACTIVE_STATUS = StringUtils.fromString("active");
    public static final BString STANDBY_STATUS = StringUtils.fromString("standby");
    public static final BString INSTANCE_ID = StringUtils.fromString("instanceId");
    public static final BString TOKEN_HOLDER = StringUtils.fromString("tokenholder");
    public static final BString LIVENESS_INTERVAL = StringUtils.fromString("livenessInterval");
    public static final String LAST_HEARTBEAT = "last_heartbeat";
    public static final String HAS_ACTIVE_TOKEN_QUERY =
            "SELECT node_id FROM token_holder WHERE node_id = ? AND is_active = true";
    public static final String HAS_TOKEN_QUERY = "SELECT node_id FROM token_holder WHERE node_id = ?";
    public static final String UPSERT_INVALID_TOKEN_QUERY = "INSERT INTO token_holder(node_id, term, is_active) " +
            "VALUES (?, 1, false) ON DUPLICATE KEY UPDATE is_active = VALUES(is_active)";
    public static final String CHECK_ACTIVE_TOKEN_QUERY = "SELECT node_id FROM token_holder WHERE is_active = true " +
            "AND term = (SELECT MAX(term) as term FROM token_holder)";
    public static final String INSERT_TOKEN_QUERY = "INSERT INTO token_holder(node_id, term, is_active) " +
            "VALUES (?, 1, true)";
    public static final String CURRENT_TIMESTAMP_QUERY = "SELECT CURRENT_TIMESTAMP";
    public static final String HEALTH_CHECK_QUERY = "SELECT last_heartbeat FROM health_check WHERE node_id = ? " +
            "ORDER BY last_heartbeat DESC LIMIT 1";
    public static final String INVALIDATE_TOKEN_QUERY = "UPDATE token_holder SET is_active = false, " +
            "term = ? WHERE node_id != ?";
    public static final String NODE_ID = "node_id";
    public static final String UPSERT_VALID_TOKEN_QUERY = "INSERT INTO token_holder(node_id, term, is_active) " +
            "VALUES (?, ?, true) ON DUPLICATE KEY UPDATE is_active = true, term = ?";
    public static final String GET_MAX_TERM_QUERY = "SELECT COALESCE(MAX(term), 0) FROM token_holder";
    public static final String UPSERT_TOKEN = "INSERT INTO token_holder (node_id, term, is_active) " +
            "SELECT ?, COALESCE(MAX(term), 0), false FROM token_holder ON DUPLICATE KEY " +
            "UPDATE is_active = false, term = COALESCE((SELECT MAX(term) FROM token_holder), 0)";

    private TokenAcquisition() { }

    /**
     * Acquires token with proper transaction handling.
     */
    public static Object acquireToken(BMap<Object, Object> databaseConfig,
                                                     BString id, boolean tokenAcquired, int livenessInterval,
                                                     int heartbeatFrequency) throws SQLException {
        DatabaseConfig dbConfig = new DatabaseConfig(
                databaseConfig.getStringValue(DB_HOST).getValue(), databaseConfig.getStringValue(DB_USER).getValue(),
                databaseConfig.getStringValue(DB_PASSWORD).getValue(), databaseConfig.getIntValue(DB_PORT).intValue(),
                databaseConfig.getStringValue(DATABASE).getValue(), null
        );
        Connection connection = null;
        try {
            String instanceId = id.getValue();
            String jdbcUrl = String.format(JDBC_URL, dbConfig.host(), dbConfig.port(), dbConfig.database());
            connection = DriverManager.getConnection(jdbcUrl, dbConfig.user(), dbConfig.password());
            connection.setAutoCommit(false);
            PreparedStatement stmt = connection.prepareStatement(HAS_TOKEN_QUERY);
            stmt.setString(1, instanceId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                throw Utils.createTaskError("Node id already exists: " + instanceId);
            }
            tokenAcquired = attemptTokenAcquisition(connection, instanceId, tokenAcquired, livenessInterval);
            connection.commit();
            HealthCheckScheduler.startHealthCheckUpdater(dbConfig, instanceId, heartbeatFrequency);
            return generateResponse(tokenAcquired, livenessInterval, instanceId, dbConfig);
        } catch (Exception e) {
            handleRollback(connection);
            throw Utils.createTaskError(e.getMessage());
        }
    }

    private static BMap<BString, Object> generateResponse(boolean tokenAcquired, int interval, String instanceId,
                                                          DatabaseConfig databaseConfig) {
        BMap<BString, Object> response = ValueCreator.createMapValue();
        response.put(STATUS, tokenAcquired ? ACTIVE_STATUS : STANDBY_STATUS);
        response.put(INSTANCE_ID, StringUtils.fromString(instanceId));
        response.put(TOKEN_HOLDER, tokenAcquired);
        response.put(DATABASE_CONFIG, databaseConfig);
        response.put(LIVENESS_INTERVAL, interval);
        return response;
    }

    public static void upsertToken(Connection connection, String instanceId) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(UPSERT_INVALID_TOKEN_QUERY)) {
            stmt.setString(1, instanceId);
            stmt.executeUpdate();
        }
    }

    public static boolean hasActiveToken(Connection connection, String instanceId) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(HAS_ACTIVE_TOKEN_QUERY)) {
            stmt.setString(1, instanceId);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        }
    }

    public static boolean attemptTokenAcquisition(Connection connection, String instanceId,
                                                  boolean hasToken, int livenessInterval) throws SQLException {
        boolean acquiredToken = hasToken;
        try (PreparedStatement stmt = connection.prepareStatement(CHECK_ACTIVE_TOKEN_QUERY)) {
            ResultSet rs = stmt.executeQuery();
            if (!rs.next()) {
                PreparedStatement insertStmt = connection.prepareStatement(INSERT_TOKEN_QUERY);
                insertStmt.setString(1, instanceId);
                insertStmt.executeUpdate();
                return true;
            }
            String existingTokenId = rs.getString(NODE_ID);
            if (existingTokenId.equals(instanceId)) {
                return true;
            }
            PreparedStatement insertStatement = connection.prepareStatement(UPSERT_TOKEN);
            insertStatement.setString(1, instanceId);
            insertStatement.executeUpdate();

            try (PreparedStatement prepareStatement = connection.prepareStatement(CURRENT_TIMESTAMP_QUERY)) {
                ResultSet response = prepareStatement.executeQuery();
                Timestamp timestamp = response.next()
                        ? response.getTimestamp(1) : Timestamp.from(Instant.now());
                PreparedStatement healthStmt = connection.prepareStatement(HEALTH_CHECK_QUERY);
                healthStmt.setString(1, existingTokenId);
                ResultSet healthCheckRs = healthStmt.executeQuery();
                int currentTerm = 1;
                if (healthCheckRs.next()) {
                    Timestamp lastHeartbeat = healthCheckRs.getTimestamp(LAST_HEARTBEAT);
                    long timeDiffSeconds = (timestamp.getTime() - lastHeartbeat.getTime()) / 1000;
                    if (timeDiffSeconds <= livenessInterval) {
                        return acquiredToken;
                    }
                    PreparedStatement maxTermQuery = connection.prepareStatement(GET_MAX_TERM_QUERY);
                    ResultSet maxTerm = maxTermQuery.executeQuery();
                    if (maxTerm.next()) {
                        currentTerm = maxTerm.getInt(1) + 1;
                    }
                    PreparedStatement deactivateStmt = connection.prepareStatement(INVALIDATE_TOKEN_QUERY);
                    deactivateStmt.setInt(1, currentTerm);
                    deactivateStmt.setString(2, instanceId);
                    deactivateStmt.executeUpdate();

                    PreparedStatement tokenStatement = connection.prepareStatement(UPSERT_VALID_TOKEN_QUERY);
                    tokenStatement.setString(1, instanceId);
                    tokenStatement.setInt(2, currentTerm);
                    tokenStatement.setInt(3, currentTerm);
                    tokenStatement.executeUpdate();
                    acquiredToken = true;
                }
            }
        }
        return acquiredToken;
    }
}
