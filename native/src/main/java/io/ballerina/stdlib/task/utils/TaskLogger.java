/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package io.ballerina.stdlib.task.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.LogManager;

/**
 * Base class for the other log functions, containing a getter to retrieve the correct logger, given a package name.
 *
 * @since 2.0.0
 */
public class TaskLogger {

    private static boolean isExistingLoggerCleared = false;
    private static boolean isTaskLoggerCreated = false;
    private static Logger logger;

    public static void clearGlobalLogger() {
        if (!isExistingLoggerCleared || isTaskLoggerCreated) {
            LogManager.getLogManager().reset();
            isExistingLoggerCleared = true;
            isTaskLoggerCreated = false;
        }
    }

    public static Logger getTaskLogger() {
        if (!isTaskLoggerCreated) {
            Path path = Paths.get(TaskConstants.LOG_FILE_PATH).toAbsolutePath();
            File file = new File(path.toString());
            try (FileInputStream fileStream = new FileInputStream(file)) {
                LogManager.getLogManager().readConfiguration(fileStream);
            } catch (IOException e) {
                PrintStream console = System.err;
                console.println(e.getMessage());
            }
            logger = LoggerFactory.getLogger("Task");
            isTaskLoggerCreated = true;
        }
        return logger;
    }
}
