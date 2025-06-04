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
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 *  OF ANY KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package io.ballerina.stdlib.task.coordination;

/**
 * Represents the configuration required to connect to a database.
 *
 * @param host     The hostname of the database server.
 * @param user     The username for authenticating with the database.
 * @param password The password for authenticating with the database.
 * @param port     The port number on which the database server is listening.
 * @param database The name of the database to connect to
 * @param dbType  The type of the database (e.g., MySQL, PostgreSQL).
 */
public record DatabaseConfig(String host, String user, String password, int port, String database, String dbType) { }
