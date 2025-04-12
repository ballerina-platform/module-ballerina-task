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

import static io.ballerina.stdlib.task.HealthCheckScheduler.startHealthCheckUpdater;

public class TokenAcquisition {

    public static final BString DB_HOST = StringUtils.fromString("host");
    public static final BString DB_USER = StringUtils.fromString("user");
    public static final BString DB_PASSWORD = StringUtils.fromString("password");
    public static final BString DB_PORT = StringUtils.fromString("port");
    public static final BString DATABASE = StringUtils.fromString("database");
    public static final String JDBC_URL = "jdbc:mysql://%s:%d/%s";
    public static final BString STATUS = StringUtils.fromString("status");
    public static final BString ACTIVE_STATUS = StringUtils.fromString("active");
    public static final BString STANDBY_STATUS = StringUtils.fromString("standby");
    public static final BString INSTANCE_ID = StringUtils.fromString("instanceId");
    public static final BString TOKEN_HOLDER = StringUtils.fromString("tokenholder");
    public static final BString CONNECTION = StringUtils.fromString("connection");
    public static final BString LIVENESS_INTERVAL = StringUtils.fromString("livenessInterval");
    public static final String LAST_HEARTBEAT = "last_heartbeat";

    public static class Error extends Exception {
        public Error(String message) {
            super(message);
        }
    }

    public static BMap<BString, Object> acquireToken(BMap<Object, Object> databaseConfig,
                                                     BString id, boolean tokenAcquired, int livenessInterval) {
        DatabaseConfig dbConfig = new DatabaseConfig(
                databaseConfig.getStringValue(DB_HOST).getValue(), databaseConfig.getStringValue(DB_USER).getValue(),
                databaseConfig.getStringValue(DB_PASSWORD).getValue(), databaseConfig.getIntValue(DB_PORT).intValue(),
                databaseConfig.getStringValue(DATABASE).getValue(), null
        );
        try {
            String instanceId = id.getValue();
            String jdbcUrl = String.format(JDBC_URL, dbConfig.host(), dbConfig.port(), dbConfig.database());
            Connection connection = DriverManager.getConnection(jdbcUrl, dbConfig.user(), dbConfig.password());
            connection.setAutoCommit(false);
            tokenAcquired = attemptTokenAcquisition(connection, instanceId, tokenAcquired, livenessInterval);
            connection.commit();
            startHealthCheckUpdater(connection, instanceId, 1);
            if (!tokenAcquired) {
                upsertToken(connection, instanceId);
            }
            return generateResponse(tokenAcquired, livenessInterval, instanceId, connection);
        } catch (Exception e) {
            throw Utils.createTaskError(e.getMessage());
        }
    }

    private static BMap<BString, Object> generateResponse(boolean tokenAcquired, int interval, String myInstanceId,
                                                          Connection connection) {
        BMap<BString, Object> response = ValueCreator.createMapValue();
        response.put(STATUS, tokenAcquired ? ACTIVE_STATUS : STANDBY_STATUS);
        response.put(INSTANCE_ID, StringUtils.fromString(myInstanceId));
        response.put(TOKEN_HOLDER, tokenAcquired);
        response.put(CONNECTION, connection);
        response.put(LIVENESS_INTERVAL, interval);
        return response;
    }

    public static void upsertToken(Connection connection, String myInstanceId) throws SQLException {
        String sql = "INSERT INTO token_holder(token_id, is_active) VALUES (?, false) " +
                "ON DUPLICATE KEY UPDATE is_active = VALUES(is_active)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, myInstanceId);
            stmt.executeUpdate();
            connection.commit();
        }
    }


    public static boolean attemptTokenAcquisition(Connection connection, String myInstanceId,
                                                  boolean acquired, int livenessInterval) throws SQLException {
        boolean tokenAcquired = acquired;
        String checkSql = "SELECT token_id FROM token_holder WHERE is_active = true";

        try (PreparedStatement stmt = connection.prepareStatement(checkSql)) {
            ResultSet rs = stmt.executeQuery();

            if (!rs.next()) {
                String insertTokenSql = "INSERT INTO token_holder(token_id, is_active) VALUES (?, true)";
                try (PreparedStatement insertStmt = connection.prepareStatement(insertTokenSql)) {
                    insertStmt.setString(1, myInstanceId);
                    insertStmt.executeUpdate();
                }
                tokenAcquired = true;
            } else {
                String existingTokenId = rs.getString("token_id");

                if (existingTokenId.equals(myInstanceId)) {
                    tokenAcquired = true;
                } else {
                    String sql = "SELECT CURRENT_TIMESTAMP";
                    PreparedStatement prepareStatement = connection.prepareStatement(sql);
                    ResultSet response = prepareStatement.executeQuery();
                    Timestamp timestamp;
                    if (response.next()) {
                        timestamp = response.getTimestamp(1);
                    } else {
                        timestamp = Timestamp.from(Instant.now());
                    }

                    String healthCheckSql = "SELECT last_heartbeat FROM health_check WHERE token_id = ? " +
                            "ORDER BY last_heartbeat DESC LIMIT 1";

                    try (PreparedStatement healthStmt = connection.prepareStatement(healthCheckSql)) {
                        healthStmt.setString(1, existingTokenId);
                        ResultSet healthCheckRs = healthStmt.executeQuery();

                        if (healthCheckRs.next()) {
                            Timestamp lastHeartbeat = healthCheckRs.getTimestamp(LAST_HEARTBEAT);
                            long timeDiffSeconds = (timestamp.getTime() - lastHeartbeat.getTime()) / 1000;

                            if (timeDiffSeconds > livenessInterval) {
                                String deactivateSql = "UPDATE token_holder SET is_active = false WHERE token_id = ?";
                                try (PreparedStatement deactivateStmt = connection.prepareStatement(deactivateSql)) {
                                    deactivateStmt.setString(1, existingTokenId);
                                    deactivateStmt.executeUpdate();
                                }
                                String checkExistingSql = "SELECT token_id FROM token_holder WHERE token_id = ?";
                                boolean hasExistingRecord;

                                try (PreparedStatement checkExistingStmt
                                             = connection.prepareStatement(checkExistingSql)) {
                                    checkExistingStmt.setString(1, myInstanceId);
                                    ResultSet existingRs = checkExistingStmt.executeQuery();
                                    hasExistingRecord = existingRs.next();
                                }

                                if (hasExistingRecord) {
                                    String updateSql = "UPDATE token_holder SET is_active = true WHERE token_id = ?";
                                    try (PreparedStatement updateStmt = connection.prepareStatement(updateSql)) {
                                        updateStmt.setString(1, myInstanceId);
                                        updateStmt.executeUpdate();
                                    }
                                } else {
                                    String insertTokenSql = "INSERT INTO token_holder(token_id, is_active) " +
                                            "VALUES (?, true)";
                                    try (PreparedStatement insertStmt = connection.prepareStatement(insertTokenSql)) {
                                        insertStmt.setString(1, myInstanceId);
                                        insertStmt.executeUpdate();
                                    }
                                }
                                tokenAcquired = true;
                            }
                        }
                    }
                }
            }
        }

        return tokenAcquired;
    }
}
