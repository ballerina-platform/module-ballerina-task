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

import io.ballerina.runtime.TypeChecker;
import io.ballerina.runtime.api.ErrorCreator;
import io.ballerina.runtime.api.StringUtils;
import io.ballerina.runtime.api.types.AttachedFunctionType;
import io.ballerina.runtime.api.types.Type;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;

import org.ballerinalang.stdlib.task.exceptions.SchedulingException;
import org.ballerinalang.stdlib.task.objects.Appointment;
import org.ballerinalang.stdlib.task.objects.ServiceInformation;
import org.ballerinalang.stdlib.task.objects.Timer;
import org.quartz.CronExpression;

/**
 * Utility functions used in ballerina task module.
 *
 * @since 0.995.0
 */
public class Utils {

    // valid resource count is set to to because the init function is also received as
    // an attached function.
    private static final int VALID_RESOURCE_COUNT = 1;

    public static BError createTaskError(String message) {
        return createTaskError(TaskConstants.LISTENER_ERROR, message);
    }

    public static BError createTaskError(String reason, String message) {
        return ErrorCreator.createDistinctError(reason, TaskConstants.TASK_PACKAGE_ID,
                StringUtils.fromString(message));
    }

    @SuppressWarnings("unchecked")
    public static String getCronExpressionFromAppointmentRecord(Object record) throws SchedulingException {
        String cronExpression = record.toString();
        if (!CronExpression.isValidExpression(cronExpression)) {
            throw new SchedulingException("Cron Expression \"" + cronExpression + "\" is invalid.");
        }
        return cronExpression;
    }

    /*
     * TODO: Runtime validation is done as compiler plugin does not work right now.
     *       When compiler plugins can be run for the resources without parameters, this will be redundant.
     *       Issue: https://github.com/ballerina-platform/ballerina-lang/issues/14148
     */
    public static void validateService(ServiceInformation serviceInformation) throws SchedulingException {
        AttachedFunctionType[] resources = serviceInformation.getService().getType().getAttachedFunctions();
        if (resources.length != VALID_RESOURCE_COUNT) {
            throw new SchedulingException("Invalid number of resources found in service \'" +
                    serviceInformation.getServiceName() + "\'. Task service should include only one resource.");
        }
        AttachedFunctionType resource = resources[0];

        if (TaskConstants.RESOURCE_ON_TRIGGER.equals(resource.getName())) {
            validateOnTriggerResource(resource.getReturnParameterType());
        } else {
            throw new SchedulingException("Invalid resource function found: " + resource.getName()
                                                  + ". Expected: \'" + TaskConstants.RESOURCE_ON_TRIGGER + "\'.");
        }
    }

    private static void validateOnTriggerResource(Type returnParameterType) throws SchedulingException {
        if (returnParameterType != io.ballerina.runtime.api.PredefinedTypes.TYPE_NULL) {
            throw new SchedulingException("Invalid resource function signature: \'" +
                    TaskConstants.RESOURCE_ON_TRIGGER + "\' should not return a value.");
        }
    }

    public static Timer processTimer(BMap<BString, Object> configurations) throws SchedulingException {
        Timer task;
        long interval = configurations.getIntValue(TaskConstants.FIELD_INTERVAL).intValue();
        long delay = configurations.getIntValue(TaskConstants.FIELD_DELAY).intValue();
        long thresholdInMillis = configurations.getIntValue(TaskConstants.THRESHOLD_IN_MILLIS).intValue();
        String misfirePolicy = String.valueOf(configurations.getStringValue(TaskConstants.MISFIRE_POLICY));

        if (configurations.get(TaskConstants.FIELD_NO_OF_RUNS) == null) {
            task = new Timer(delay, interval, thresholdInMillis, misfirePolicy);
        } else {
            long noOfRuns = configurations.getIntValue(TaskConstants.FIELD_NO_OF_RUNS);
            task = new Timer(delay, interval, thresholdInMillis, misfirePolicy, noOfRuns);
        }
        return task;
    }

    public static Appointment processAppointment(BMap<BString, Object> configurations)
            throws SchedulingException {
        Appointment appointment;
        Object appointmentDetails = configurations.get(TaskConstants.MEMBER_CRON_EXPRESSION);
        String cronExpression = getCronExpressionFromAppointmentRecord(appointmentDetails);
        long thresholdInMillis = configurations.getIntValue(TaskConstants.THRESHOLD_IN_MILLIS).intValue();
        String misfirePolicy = String.valueOf(configurations.getStringValue(TaskConstants.MISFIRE_POLICY));

        if (configurations.get(TaskConstants.FIELD_NO_OF_RUNS) == null) {
            appointment = new Appointment(cronExpression, thresholdInMillis, misfirePolicy);
        } else {
            long noOfRuns = configurations.getIntValue(TaskConstants.FIELD_NO_OF_RUNS);
            appointment = new Appointment(cronExpression, thresholdInMillis, misfirePolicy, noOfRuns);
        }
        return appointment;
    }
}
