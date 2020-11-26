/*
 *  Copyright (c) 2019 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.ballerinalang.stdlib.task.utils;

import io.ballerina.runtime.api.Runtime;
import io.ballerina.runtime.api.async.StrandMetadata;
import io.ballerina.runtime.api.values.BObject;
import org.ballerinalang.stdlib.task.objects.ServiceInformation;

import static io.ballerina.runtime.api.constants.RuntimeConstants.BALLERINA_BUILTIN_PKG_PREFIX;

/**
 * This class invokes the Ballerina onTrigger function, and if an error occurs while invoking that function, it invokes
 * the onError function.
 */
public class TaskExecutor {

    private static final StrandMetadata TASK_METADATA =
            new StrandMetadata(BALLERINA_BUILTIN_PKG_PREFIX, TaskConstants.PACKAGE_NAME, TaskConstants.PACKAGE_VERSION,
                    TaskConstants.RESOURCE_ON_TRIGGER);

    public static void executeFunction(ServiceInformation serviceInformation) {

        Runtime runtime = serviceInformation.getRuntime();
        runtime.invokeMethodAsync(serviceInformation.getService(), TaskConstants.RESOURCE_ON_TRIGGER, null,
                TASK_METADATA, null);
    }

    public static void executeFunction(BObject job, Runtime runtime) {
        runtime.invokeMethodAsync(job, TaskConstants.RESOURCE_ON_TRIGGER, null,
                TASK_METADATA, null);
    }
}
