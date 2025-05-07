// Copyright (c) 2025 WSO2 LLC. (http://www.wso2.com).
//
// WSO2 LLC. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

import ballerina/jballerina.java;
import ballerina/uuid;
import ballerina/log;

# Listener for task scheduling.
public class Listener {
    private ListenerConfiguration config;
    
    # Initializes the task 'listener.
    # 
    # + config - The 'listener configuration
    public isolated function init(*ListenerConfiguration config) {
        self.config = config;
        initListener(self, config);
    }
    
    # Attaches a service to the 'listener.
    # 
    # + s - The service to be attached
    # + name - The service name
    # + return - An error if the service cannot be attached
    public isolated function attach(Service s, string[]|string? name = ()) returns error? {
        if name is string[] {
            foreach string serviceId in name {
                error? err = attachService(self, s, serviceId);
                log:printError("Error attaching service: ", err);
            }
        } else {
            string serviceId = name is string ? name : uuid:createRandomUuid();
            return attachService(self, s, serviceId);
        }
    }
    
    # Detaches a service from the 'listener.
    # 
    # + s - The service to be detached
    # + return - An error if the service cannot be detached
    public isolated function detach(Service s) returns error? {
        // return detachService(self, s);
    }
    
    # Starts the 'listener.
    # 
    # + return - An error if the 'listener fails to start
    public isolated function 'start() returns error? {
        return startListener(self);
    }

    # Stops the 'listener gracefully.
    # 
    # + return - An error if the 'listener fails to stop
    public isolated function gracefulStop() returns error? {
        // return gracefulStopListener(self);
    }
    
    # Stops the 'listener immediately.
    # 
    # + return - An error if the 'listener fails to stop
    public isolated function immediateStop() returns error? {
        return immediateStopListener(self);
    }
}

isolated function initListener(Listener 'listener, ListenerConfiguration config) = @java:Method {
    'class: "io.ballerina.stdlib.task.listener.ListenerAction"
} external;

isolated function attachService(Listener 'listener, Service s, string serviceId) returns error? = @java:Method {
    'class: "io.ballerina.stdlib.task.listener.ListenerAction"
} external;

isolated function startListener(Listener 'listener) returns error? = @java:Method {
    'class: "io.ballerina.stdlib.task.listener.ListenerAction"
} external;

isolated function immediateStopListener(Listener 'listener) returns error? = @java:Method {
    'class: "io.ballerina.stdlib.task.listener.ListenerAction"
} external;
