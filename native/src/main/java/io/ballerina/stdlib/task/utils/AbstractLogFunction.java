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

import io.ballerina.runtime.api.values.BString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * Base class for the other log functions, containing a getter to retrieve the correct logger, given a package name.
 *
 * @since 2.0.0
 */
public class AbstractLogFunction {

    private static final String BALLERINA_ROOT_LOGGER_NAME = "ballerina";
    private static final Logger ballerinaRootLogger = LoggerFactory.getLogger(BALLERINA_ROOT_LOGGER_NAME);

    protected static Logger getLogger(String pkg) {
        if (".".equals(pkg) || pkg == null) {
            return ballerinaRootLogger;
        } else {
            // TODO: Refactor this later
            return LoggerFactory.getLogger(ballerinaRootLogger.getName() + "." + pkg);
        }
    }

    /**
     * Execute logging provided message.
     *
     * @param message  log message
     * @param pckg     package
     * @param consumer log message consumer
     */
    static void logMessage(BString message, String pckg,
                           BiConsumer<String, String> consumer, String outputFormat) {
        // Create a new log message supplier
        Supplier<String> logMessage = new Supplier<>() {
            private String msg = null;

            @Override
            public String get() {
                // We should invoke the lambda only once, thus caching return value
                if (msg == null) {
                    msg = message.toString();
                }
                return msg;
            }
        };
        consumer.accept(pckg, logMessage.get());
    }
}
