DROP TABLE IF EXISTS token_holder;
CREATE TABLE token_holder (
    task_id VARCHAR(255) NOT NULL,
    group_id VARCHAR(255) NOT NULL PRIMARY KEY,
    term INT NOT NULL
);

DROP TABLE IF EXISTS health_check;
CREATE TABLE health_check (
    task_id       VARCHAR(255),
    group_id      VARCHAR(36),
    last_heartbeat TIMESTAMP,
    PRIMARY KEY (task_id, group_id)
);

DROP TABLE IF EXISTS payments;
CREATE TABLE payments (
    id SERIAL PRIMARY KEY,
    message VARCHAR(255)
);

DROP TABLE IF EXISTS orders;
CREATE TABLE orders (
    id SERIAL PRIMARY KEY,
    message VARCHAR(255)
);

-- Create user and database
-- Run these as a superuser or a user with appropriate privileges

-- Create user only if it doesn't exist (PostgreSQL doesn't support `IF NOT EXISTS` for CREATE USER directly)
DO
$$
BEGIN
    IF NOT EXISTS (
        SELECT FROM pg_catalog.pg_roles WHERE rolname = 'root'
    ) THEN
        CREATE ROLE root WITH LOGIN PASSWORD 'password';
    END IF;
END
$$;

-- Drop database if it already exists
DROP DATABASE IF EXISTS testdb;

-- Create the database owned by root
CREATE DATABASE testdb OWNER root;
