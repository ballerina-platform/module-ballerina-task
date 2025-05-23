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

# Listener for task scheduling.
public isolated class Listener {
    
    # Initializes the task 'listener.
    # 
    # + config - The 'listener configuration
    public isolated function init(*ListenerConfiguration config) returns Error? {
        check self.initListener(config);
    }
    
    # Attaches a service to the 'listener.
    # 
    # + s - The service to be attached
    # + name - The service name
    # + return - An error if the service cannot be attached
    public isolated function attach(Service s, string[]|string? name = ()) returns Error? {
        string serviceId = name is string ? name : (name is string[] ? name[0] : uuid:createRandomUuid());
        return attachService(self, s, serviceId);
    }
    
    # Detaches a service from the 'listener.
    # 
    # + s - The service to be detached
    # + return - An error if the service cannot be detached
    public isolated function detach(Service s) returns Error? = @java:Method {
        'class: "io.ballerina.stdlib.task.listener.ListenerAction"
    } external;
    
    # Starts the 'listener.
    # 
    # + return - An error if the 'listener fails to start
    public isolated function 'start() returns Error? = @java:Method {
        'class: "io.ballerina.stdlib.task.listener.ListenerAction"
    } external;

    # Stops the 'listener gracefully.
    # 
    # + return - An error if the 'listener fails to stop
    public isolated function gracefulStop() returns Error? = @java:Method {
        'class: "io.ballerina.stdlib.task.listener.ListenerAction"
    } external;
    
    # Stops the 'listener immediately.
    # 
    # + return - An error if the 'listener fails to stop
    public isolated function immediateStop() returns Error? = @java:Method {
        name: "gracefulStop",
        'class: "io.ballerina.stdlib.task.listener.ListenerAction"
    } external;

    isolated function initListener(ListenerConfiguration config) returns Error? = @java:Method {
        'class: "io.ballerina.stdlib.task.listener.ListenerAction"
    } external;
}

isolated function attachService(Listener 'listener, Service s, string serviceId) returns Error? = @java:Method {
    'class: "io.ballerina.stdlib.task.listener.ListenerAction"
} external;

