-- ============================================================
--  INFANT GEOFENCE ALERT SYSTEM — MySQL Database Schema
--  Import via phpMyAdmin or: mysql -u root < schema.sql
-- ============================================================

CREATE DATABASE IF NOT EXISTS infant_geofence
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE infant_geofence;

-- ── Users (parents) ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS users (
  id           INT AUTO_INCREMENT PRIMARY KEY,
  name         VARCHAR(100)        NOT NULL,
  email        VARCHAR(150)        UNIQUE NOT NULL,
  password     VARCHAR(255)        NOT NULL,
  fcm_token    TEXT                DEFAULT NULL,  -- Firebase push token
  created_at   TIMESTAMP           DEFAULT CURRENT_TIMESTAMP
);

-- ── Devices ─────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS devices (
  id             INT AUTO_INCREMENT PRIMARY KEY,
  device_id      VARCHAR(50)       UNIQUE NOT NULL,
  child_name     VARCHAR(100)      NOT NULL,
  parent_user_id INT               NOT NULL,
  battery_pct    TINYINT UNSIGNED  DEFAULT NULL,
  last_seen      TIMESTAMP         DEFAULT NULL,
  created_at     TIMESTAMP         DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (parent_user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- ── Locations ────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS locations (
  id          INT AUTO_INCREMENT PRIMARY KEY,
  device_id   VARCHAR(50)     NOT NULL,
  latitude    DECIMAL(10, 8)  NOT NULL,
  longitude   DECIMAL(11, 8)  NOT NULL,
  speed_kmph  FLOAT           DEFAULT 0,
  satellites  TINYINT         DEFAULT 0,
  hdop        FLOAT           DEFAULT 99.9,
  timestamp   TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_device_time (device_id, timestamp)
);

-- ── Geofences ────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS geofences (
  id                    INT AUTO_INCREMENT PRIMARY KEY,
  device_id             VARCHAR(50)   NOT NULL,
  name                  VARCHAR(100)  DEFAULT 'Safe Zone',
  boundary_coordinates  JSON          NOT NULL,  -- array of {lat, lng}
  is_active             TINYINT(1)    DEFAULT 1,
  created_at            TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_device (device_id)
);

-- ── Alerts ───────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS alerts (
  id          INT AUTO_INCREMENT PRIMARY KEY,
  device_id   VARCHAR(50)                             NOT NULL,
  alert_type  ENUM('inside','outside','sos','low_battery') NOT NULL,
  latitude    DECIMAL(10, 8)                          DEFAULT NULL,
  longitude   DECIMAL(11, 8)                          DEFAULT NULL,
  is_read     TINYINT(1)                              DEFAULT 0,
  timestamp   TIMESTAMP                               DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_device_time (device_id, timestamp)
);

-- ── Sample Data (optional, for testing) ─────────────────────
INSERT INTO users (name, email, password) VALUES
  ('Test Parent', 'parent@test.com', '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi');
  -- password = "password"

INSERT INTO devices (device_id, child_name, parent_user_id) VALUES
  ('ESP32_001', 'Baby Alex', 1);
