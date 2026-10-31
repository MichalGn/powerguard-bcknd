-- =====================================================================
-- POWERGUARD - COMPLETE DATABASE SCHEMA (v405 + 501 p7; SERVICE role)
-- Client -> Site -> Edge -> Meter hierarchy
-- Fresh installation / database rebuild (including roles and trusted display devices)
--
-- IMPORTANT:
--   * This script is DESTRUCTIVE: it drops existing PowerGuard tables.
--   * Run it while connected to database: powerguarddb
--   * PostgreSQL role programpowerguard must already exist.
--   * Removes ALL existing PowerGuard application data and accounts.
--   * Does not drop/recreate the database itself, preserving its connection settings.
-- =====================================================================

BEGIN;

-- =====================================================================
-- CLEANUP
-- =====================================================================
DROP TABLE IF EXISTS trusted_display_devices CASCADE;
DROP TABLE IF EXISTS communication_status_history CASCADE;
DROP TABLE IF EXISTS alarms CASCADE;
DROP TABLE IF EXISTS measurements CASCADE;
DROP TABLE IF EXISTS alarm_rules CASCADE;
DROP TABLE IF EXISTS meters CASCADE;
DROP TABLE IF EXISTS edges CASCADE;
DROP TABLE IF EXISTS users_sites CASCADE;
DROP TABLE IF EXISTS sites CASCADE;
DROP TABLE IF EXISTS clients CASCADE;
DROP TABLE IF EXISTS application_settings CASCADE;
DROP TABLE IF EXISTS users CASCADE;

-- =====================================================================
-- USERS
-- =====================================================================
CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(64)  NOT NULL UNIQUE,
    firstname     VARCHAR(64),
    lastname      VARCHAR(64),
    email         VARCHAR(320),
    phone         VARCHAR(32),
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(32)  NOT NULL,
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT chk_users_role CHECK (role IN ('ADMIN', 'SERVICE', 'ADVANCED_USER', 'USER'))
);
CREATE INDEX idx_users_enabled ON users(enabled);

-- The v402 role migration renames existing USER accounts to ADVANCED_USER.
-- Fresh installations support both roles but intentionally create no users.

-- =====================================================================
-- TRUSTED DISPLAY DEVICES (automatic login for authenticated users)
-- =====================================================================
CREATE TABLE trusted_display_devices (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash CHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_trusted_display_devices_expiry ON trusted_display_devices(expires_at);


-- =====================================================================
-- APPLICATION SETTINGS / END USER
-- =====================================================================
CREATE TABLE application_settings (
    id                INTEGER PRIMARY KEY,
    company_name      VARCHAR(255),
    installation_name VARCHAR(255),
    address           VARCHAR(1000),
    nip               VARCHAR(32),
    email             VARCHAR(320),
    phone             VARCHAR(64),
    contact_person    VARCHAR(255),
    auto_login_user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT chk_application_settings_singleton CHECK (id = 1)
);
INSERT INTO application_settings(id) VALUES (1);

-- =====================================================================
-- CLIENTS / KLIENCI
-- id        = internal DB key
-- client_id = stable technical ID used by MQTT / Edge
-- name      = friendly name shown to humans
-- =====================================================================
CREATE TABLE clients (
    id            BIGSERIAL PRIMARY KEY,
    client_id     VARCHAR(128) NOT NULL UNIQUE,
    name          VARCHAR(255) NOT NULL,
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    display_order INTEGER      NOT NULL DEFAULT 0,
    CONSTRAINT chk_clients_client_id_not_blank CHECK (length(trim(client_id)) > 0),
    CONSTRAINT chk_clients_name_not_blank CHECK (length(trim(name)) > 0)
);
CREATE INDEX idx_clients_enabled_order ON clients(enabled, display_order, name);

-- =====================================================================
-- SITES / ODDZIALY
-- site_id is unique inside one client.
-- =====================================================================
CREATE TABLE sites (
    id            BIGSERIAL PRIMARY KEY,
    client_fk     BIGINT       NOT NULL,
    site_id       VARCHAR(128) NOT NULL,
    name          VARCHAR(255) NOT NULL,
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    display_order INTEGER      NOT NULL DEFAULT 0,
    CONSTRAINT fk_sites_client
        FOREIGN KEY (client_fk) REFERENCES clients(id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT uq_sites_client_site UNIQUE (client_fk, site_id),
    CONSTRAINT chk_sites_site_id_not_blank CHECK (length(trim(site_id)) > 0),
    CONSTRAINT chk_sites_name_not_blank CHECK (length(trim(name)) > 0)
);
CREATE INDEX idx_sites_client_order ON sites(client_fk, enabled, display_order, name);
CREATE INDEX idx_sites_site_id ON sites(site_id);

-- =====================================================================
-- USER <-> SITE ACCESS
-- ADMIN has implicit access to all sites; assignments are used by
-- SERVICE, ADVANCED_USER and USER accounts as permitted by the backend.
-- Internal numeric site key is used so equal site_id values may exist
-- under different clients without ambiguity.
-- =====================================================================
CREATE TABLE users_sites (
    user_id BIGINT NOT NULL,
    site_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, site_id),
    CONSTRAINT fk_users_sites_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_users_sites_site
        FOREIGN KEY (site_id) REFERENCES sites(id) ON DELETE CASCADE
);
CREATE INDEX idx_users_sites_site ON users_sites(site_id);

-- =====================================================================
-- EDGES / COMPUTERS BRZEGOWYCH
-- Edge is technical infrastructure; no human-facing name is required.
-- edge_id is unique inside one site.
-- =====================================================================
CREATE TABLE edges (
    id        BIGSERIAL PRIMARY KEY,
    site_fk   BIGINT       NOT NULL,
    edge_id   VARCHAR(128) NOT NULL,
    CONSTRAINT fk_edges_site
        FOREIGN KEY (site_fk) REFERENCES sites(id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT uq_edges_site_edge UNIQUE (site_fk, edge_id),
    CONSTRAINT chk_edges_edge_id_not_blank CHECK (length(trim(edge_id)) > 0)
);
CREATE INDEX idx_edges_site ON edges(site_fk, edge_id);

-- =====================================================================
-- METERS / LICZNIKI
-- unique_key is the technical MeterId used by MQTT / Edge.
-- name is the friendly display name shown to humans.
-- =====================================================================
CREATE TABLE meters (
    id            SERIAL PRIMARY KEY,
    edge_fk       BIGINT       NOT NULL,
    unique_key    VARCHAR(128) NOT NULL,
    name          VARCHAR(255) NOT NULL,
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    display_order INTEGER      NOT NULL DEFAULT 0,
    CONSTRAINT fk_meters_edge
        FOREIGN KEY (edge_fk) REFERENCES edges(id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT uq_meters_edge_unique_key UNIQUE (edge_fk, unique_key),
    CONSTRAINT chk_meters_unique_key_not_blank CHECK (length(trim(unique_key)) > 0),
    CONSTRAINT chk_meters_name_not_blank CHECK (length(trim(name)) > 0)
);
CREATE INDEX idx_meters_enabled ON meters(enabled);
CREATE INDEX idx_meters_edge_order ON meters(edge_fk, display_order, id);

-- =====================================================================
-- ALARM RULES
-- =====================================================================
CREATE TABLE alarm_rules (
    id                           SERIAL PRIMARY KEY,
    meter_id                     INTEGER NOT NULL UNIQUE,
    instantaneous_alarm_on_kw    NUMERIC(16,6),
    instantaneous_alarm_off_kw   NUMERIC(16,6),
    quarter_hour_alarm_on_kw     NUMERIC(16,6),
    quarter_hour_alarm_off_kw    NUMERIC(16,6),
    enabled                      BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_alarm_rules_meter
        FOREIGN KEY (meter_id) REFERENCES meters(id) ON DELETE CASCADE,
    CONSTRAINT chk_instant_alarm_pair CHECK (
        (instantaneous_alarm_on_kw IS NULL AND instantaneous_alarm_off_kw IS NULL)
        OR
        (instantaneous_alarm_on_kw IS NOT NULL AND instantaneous_alarm_off_kw IS NOT NULL
         AND instantaneous_alarm_on_kw >= 0
         AND instantaneous_alarm_off_kw >= 0
         AND instantaneous_alarm_off_kw < instantaneous_alarm_on_kw)
    ),
    CONSTRAINT chk_quarter_alarm_pair CHECK (
        (quarter_hour_alarm_on_kw IS NULL AND quarter_hour_alarm_off_kw IS NULL)
        OR
        (quarter_hour_alarm_on_kw IS NOT NULL AND quarter_hour_alarm_off_kw IS NOT NULL
         AND quarter_hour_alarm_on_kw >= 0
         AND quarter_hour_alarm_off_kw >= 0
         AND quarter_hour_alarm_off_kw < quarter_hour_alarm_on_kw)
    )
);
CREATE INDEX idx_alarm_rules_enabled ON alarm_rules(enabled);

-- =====================================================================
-- MEASUREMENTS
-- =====================================================================
CREATE TABLE measurements (
    id                       BIGSERIAL PRIMARY KEY,
    meter_id                 INTEGER       NOT NULL,
    measured_at              TIMESTAMPTZ   NOT NULL,
    meter_measured_at        TIMESTAMPTZ,
    active_power_kw          NUMERIC(16,6),
    active_power_source      VARCHAR(40),
    active_power_quality     VARCHAR(20),
    active_energy_import_kwh NUMERIC(20,6),
    active_energy_export_kwh NUMERIC(20,6),
    current_quarter_power_kw NUMERIC(16,6),
    voltage_l1_v             NUMERIC(12,4),
    voltage_l2_v             NUMERIC(12,4),
    voltage_l3_v             NUMERIC(12,4),
    current_l1_a             NUMERIC(12,4),
    current_l2_a             NUMERIC(12,4),
    current_l3_a             NUMERIC(12,4),
    frequency_hz             NUMERIC(10,4),
    read_duration_ms         BIGINT,
    bcc_valid                BOOLEAN,
    CONSTRAINT fk_measurements_meter
        FOREIGN KEY (meter_id) REFERENCES meters(id) ON DELETE CASCADE,
    CONSTRAINT uq_measurements_meter_time UNIQUE (meter_id, measured_at)
);
CREATE INDEX idx_measurements_meter_time ON measurements(meter_id, measured_at DESC);
CREATE INDEX idx_measurements_measured_at ON measurements(measured_at DESC);

-- =====================================================================
-- ALARM INCIDENTS
-- =====================================================================
CREATE TABLE alarms (
    id              BIGSERIAL PRIMARY KEY,
    meter_id        INTEGER       NOT NULL,
    alarm_type      VARCHAR(32)   NOT NULL,
    started_at      TIMESTAMPTZ   NOT NULL,
    ended_at        TIMESTAMPTZ,
    start_value     NUMERIC(16,6),
    max_value       NUMERIC(16,6),
    acknowledged_at TIMESTAMPTZ,
    acknowledged_by VARCHAR(128),
    CONSTRAINT fk_alarms_meter
        FOREIGN KEY (meter_id) REFERENCES meters(id) ON DELETE CASCADE,
    CONSTRAINT chk_alarm_type CHECK (
        alarm_type IN ('INSTANTANEOUS_POWER', 'QUARTER_HOUR_POWER')
    ),
    CONSTRAINT chk_alarm_dates CHECK (
        ended_at IS NULL OR ended_at >= started_at
    )
);
CREATE INDEX idx_alarms_meter_started_at ON alarms(meter_id, started_at DESC);
CREATE INDEX idx_alarms_open ON alarms(meter_id, alarm_type, started_at DESC) WHERE ended_at IS NULL;
CREATE UNIQUE INDEX uq_alarms_one_open_per_type ON alarms(meter_id, alarm_type) WHERE ended_at IS NULL;

-- =====================================================================
-- COMMUNICATION STATUS HISTORY
-- Backend, Edge and meter heartbeat/status samples used by the dashboard
-- communication diagnostics timeline.
-- =====================================================================
CREATE TABLE communication_status_history (
    id               BIGSERIAL PRIMARY KEY,
    client_id        VARCHAR(128),
    site_id          VARCHAR(128),
    edge_id          VARCHAR(128),
    meter_unique_key VARCHAR(128),
    entity_type      VARCHAR(16)  NOT NULL,
    status           VARCHAR(16)  NOT NULL,
    observed_at      TIMESTAMPTZ  NOT NULL,
    CONSTRAINT chk_communication_entity_type CHECK (entity_type IN ('SERVER', 'EDGE', 'METER')),
    CONSTRAINT chk_communication_status CHECK (status IN ('ONLINE', 'DEGRADED', 'OFFLINE', 'STARTING'))
);

CREATE INDEX idx_comm_status_identity_time
    ON communication_status_history(client_id, site_id, edge_id, meter_unique_key, observed_at DESC);
CREATE INDEX idx_comm_status_observed_at
    ON communication_status_history(observed_at DESC);

-- =====================================================================
-- PRIVILEGES
-- =====================================================================
GRANT CONNECT ON DATABASE powerguarddb TO programpowerguard;
GRANT USAGE ON SCHEMA public TO programpowerguard;
GRANT SELECT, INSERT, UPDATE, DELETE ON
    users,
    application_settings,
    clients,
    sites,
    users_sites,
    edges,
    meters,
    alarm_rules,
    measurements,
    alarms,
    communication_status_history,
    trusted_display_devices
TO programpowerguard;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO programpowerguard;
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO programpowerguard;
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT USAGE, SELECT ON SEQUENCES TO programpowerguard;

COMMIT;

-- =====================================================================
-- POST-INSTALL NOTES
-- =====================================================================
-- User accounts are deliberately NOT inserted: provision through the application
-- or insert a correctly hashed password using your existing authentication flow.
-- Intended roles: ADMIN, SERVICE, ADVANCED_USER, USER.
-- Automatic-login tokens must be generated and registered by the backend.
-- Do not insert tokens manually or store plaintext tokens in this table.
-- WARNING: This fresh-install script removes all existing application data.
-- active_power_kw is SIGNED: positive = import, negative = export.
-- active_power_source and active_power_quality are populated by BE 501 p7.


---------------------------------------------------------------------------------------------------------
-- Non-destructive migration for installation-wide automatic login.
-- Run once on powerguarddb BEFORE deploying the new backend.
ALTER TABLE application_settings
    ADD COLUMN IF NOT EXISTS auto_login_user_id BIGINT REFERENCES users(id) ON DELETE SET NULL;
-- Initially disabled. Configure an enabled USER in the admin UI.
GRANT SELECT, UPDATE ON application_settings TO programpowerguard;
