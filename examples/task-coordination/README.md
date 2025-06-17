# Task Coordination in Ballerina

## Overview

This repository demonstrates the task coordination capabilities in Ballerina, designed for distributed systems where high availability is necessary. The coordination mechanism ensures that when tasks are running on multiple nodes, only one node is active while others remain on standby. If the active node fails, one of the standby nodes automatically takes over, maintaining system availability.

This repository implements an RDBMS-based coordination system that handles system availability across multiple nodes, improving the reliability and uptime of distributed applications.

## Design

The task coordination system follows a warm backup approach where,

* Multiple nodes run the same program logic on separate tasks
* One node is designated as token-bearer and execute the program logic
* Other nodes act as watchdogs by monitoring the status of the token-bearer node
* If the active node fails, one of the candidate node takes over automatically

## Configurations

The task coordination system can be configured using the `WarmBackupConfig` record under `ListenerConfiguration`. So the coordination can only be done through a task listener. This handles how each node participates in coordination, how frequently it checks for liveness, updates its status, and connects to the coordination database.

```ballerina
public type WarmBackupConfig record {
    DatabaseConfig databaseConfig = {};
    int livenessCheckInterval = 30;
    string taskId;
    string groupId;
    int heartbeatFrequency = 1;
};

public type DatabaseConfig MysqlConfig|PostgresqlConfig;
```

### Configuration Parameters

| Parameter | Description |
|-----------|-------------|
| **databaseConfig** | Database configurations for task coordination |
| **livenessCheckInterval** | Interval (in seconds) to check the liveness of the active node |
| **taskId** | Unique identifier for the current node |
| **groupId** | Identifier for the group of nodes coordinating the task |
| **heartbeatFrequency** | Interval (in seconds) for the node to update its heartbeat |

### Database Configuration

The `databaseConfig` can be either MySQL or PostgreSQL. This is defined using a union type as `DatabaseConfig`. Users can choose either `task:MysqlConfig` or `task:PostgresqlConfig` based on their preferred database.

**For PostgreSQL:**

```ballerina
type PostgresqlConfig record {
   string host;
   int port;
   string user;
   string password;
   string database;
};
```

**For MySQL:**

```ballerina
type MysqlConfig record {
   string host;
   int port;
   string user;
   string password;
   string database;
};
```

### Setting Up Task Coordination

1. **Create a Task Listener**

   First, set up a task listener with both scheduling parameters and coordination configuration:

   ```ballerina
   listener task:Listener taskListener = new (
      trigger = {
         interval,
         maxCount
      }, 
      warmBackupConfig = {
         databaseConfig,
         livenessCheckInterval,
         taskId, // must be unique for each node
         groupId,
         heartbeatFrequency
      }
   );
   ```

2. **Implement Service Logic**

   Create a service with your business logic in the `execute` method.

   ```ballerina
   service "job-1" on taskListener {
      private int i = 1;

      isolated function execute() {
         // add the business logic
      }
   }
   ```

On a different node, deploy the same code but with a different value for `taskId`:

### Database Schema

The task coordination system requires two essential tables that must be created before starting your application.

#### Token holder table

The `token_holder` table maintains information about which node is currently the active token bearer:

| Column | Type | Description |
|--------|------|-------------|
| group_id | VARCHAR(255) | Primary key, identifies the coordination group |
| task_id | VARCHAR(255) | The ID of the node |
| term | INTEGER | Increments with each leadership change, prevents split-brain scenarios |

#### Health check table

The `health_check` table stores heartbeat information for each node.

| Column | Type | Description |
|--------|------|-------------|
| task_id | VARCHAR(255) | Node identifier (part of compound primary key) |
| group_id | VARCHAR(255) | Group identifier (part of compound primary key) |
| last_heartbeat | TIMESTAMP | Last time the node sent a heartbeat |

### Execution

Ensure your PostgreSQL or MySQL database is set up and accessible from all nodes. And the coordination tables should be created before the service starts. Then deploy the same application on multiple nodes and ensure each node has a unique `taskId` but the same `groupId`. All nodes should connect to the same coordination database.

1. Navigate to the `task-coordination` example.

   ```bash
   cd examples/task-coordination
   ```

2. Start the PostgreSQL server.

   ```bash
   docker compose up
   ```

3. Configure your application by updating the `Config.toml` file.

   ```toml
   taskId = "add-unique-task"
   groupId = "task-group-01"
   heartbeatFrequency = 1
   livenessCheckInterval = 6

   [databaseConfig]
    host = "localhost"
    user = "root"
    password = "password"
    port = 5432
    database = "testdb"
   ```

4. Clone the Ballerina application (`orders-task`) on multiple nodes and set unique values for the task id.

5. Run the application

   ```bash
   bal run
   ```

### Testing Failover

To test the failover mechanism,

1. Start all the nodes and start the Ballerina application
2. Verify only one node is actively processing tasks
3. Terminate the token bearer (active) node
4. Within `livenessCheckInterval` seconds, the standby node will detect the failure
5. The standby node will acquire the token and begin processing tasks
