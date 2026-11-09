-- ========================================================================
-- Project Assignment Set 19: International Port Container Terminal & Yard Engine
-- Database Schema: port_schema.sql
-- Database: port_terminal_db
-- ========================================================================

DROP DATABASE IF EXISTS port_terminal_db;
CREATE DATABASE port_terminal_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE port_terminal_db;

-- ------------------------------------------------------------------------
-- Table 1: vessels
-- Stores inbound vessel schedules, voyage details, and docking berths
-- ------------------------------------------------------------------------
CREATE TABLE vessels (
    voyage_id VARCHAR(50) NOT NULL PRIMARY KEY,
    vessel_name VARCHAR(100) NOT NULL,
    origin_port VARCHAR(100) NOT NULL,
    destination_port VARCHAR(100) NOT NULL,
    berth_number VARCHAR(30) NOT NULL,
    arrival_date DATETIME NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'BERTHED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- ------------------------------------------------------------------------
-- Table 2: yard_slots
-- Represents spatial slots in the terminal yard for storage and stacking
-- ------------------------------------------------------------------------
CREATE TABLE yard_slots (
    slot_id VARCHAR(30) NOT NULL PRIMARY KEY,
    block_name VARCHAR(50) NOT NULL,
    tier_level INT NOT NULL DEFAULT 1,
    slot_type ENUM('REEFER', 'STANDARD_DRY', 'HAZMAT') NOT NULL,
    is_occupied BOOLEAN NOT NULL DEFAULT FALSE,
    assigned_crane_id VARCHAR(30) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- ------------------------------------------------------------------------
-- Table 3: containers
-- Stores container details across Phase 1, Phase 2, and Phase 3 data entry
-- ------------------------------------------------------------------------
CREATE TABLE containers (
    container_id VARCHAR(20) NOT NULL PRIMARY KEY,
    voyage_id VARCHAR(50) NOT NULL,
    cargo_category ENUM('REEFER', 'STANDARD_DRY', 'HAZMAT') NOT NULL,
    weight_tons DOUBLE NOT NULL,
    slot_id VARCHAR(30) NULL,
    yard_block VARCHAR(30) NOT NULL,
    customs_doc_text TEXT NULL,
    customs_seal_status ENUM('INTACT', 'TAMPERED', 'PENDING_INSPECTION') NOT NULL DEFAULT 'INTACT',
    gantry_placement_time DATETIME NULL,
    reefer_power_kw DOUBLE DEFAULT 0.0,
    off_power_minutes INT DEFAULT 0,
    dwell_days INT NOT NULL DEFAULT 1,
    free_dwell_cap INT NOT NULL DEFAULT 7,
    daily_demurrage_rate DOUBLE NOT NULL DEFAULT 150.0,
    performance_score DOUBLE DEFAULT 0.0,
    performance_band VARCHAR(60) DEFAULT 'Pending Evaluation',
    status ENUM('DISCHARGED', 'IN_YARD', 'CLEARED', 'GATE_OUT', 'BLOCKED_AUDIT') DEFAULT 'DISCHARGED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_container_voyage FOREIGN KEY (voyage_id) REFERENCES vessels(voyage_id) ON DELETE CASCADE,
    CONSTRAINT fk_container_slot FOREIGN KEY (slot_id) REFERENCES yard_slots(slot_id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- ------------------------------------------------------------------------
-- Table 4: gate_passes
-- Phase 3 clearance and gate dispatch records with demurrage audit
-- ------------------------------------------------------------------------
CREATE TABLE gate_passes (
    pass_id VARCHAR(50) NOT NULL PRIMARY KEY,
    container_id VARCHAR(20) NOT NULL,
    truck_reg_number VARCHAR(30) NOT NULL,
    driver_license VARCHAR(50) NOT NULL,
    customs_clearance_stamp VARCHAR(80) NOT NULL,
    actual_dwell_days INT NOT NULL,
    free_dwell_cap INT NOT NULL,
    daily_demurrage_rate DOUBLE NOT NULL,
    demurrage_penalty DOUBLE NOT NULL DEFAULT 0.0,
    performance_score DOUBLE NOT NULL,
    operational_band VARCHAR(60) NOT NULL,
    gate_out_time DATETIME NOT NULL,
    issued_by VARCHAR(50) NOT NULL DEFAULT 'TERMINAL_OPERATIONS_DESK',
    remarks TEXT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_gatepass_container FOREIGN KEY (container_id) REFERENCES containers(container_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ------------------------------------------------------------------------
-- Seed Data: Baseline Vessels
-- ------------------------------------------------------------------------
INSERT INTO vessels (voyage_id, vessel_name, origin_port, destination_port, berth_number, arrival_date, status) VALUES
('VOY-EVER-2026', 'Ever Given X', 'Singapore (SGSIN)', 'Chennai (INMAA)', 'BERTH-01', '2026-11-01 06:30:00', 'BERTHED'),
('VOY-MAER-8841', 'Maersk Mc-Kinney', 'Rotterdam (NLRTM)', 'Mumbai (INBOM)', 'BERTH-02', '2026-11-03 10:15:00', 'BERTHED'),
('VOY-CMA-4420', 'CMA CGM Palais', 'Shanghai (CNSHA)', 'Tuticorin (INTUT)', 'BERTH-03', '2026-11-05 14:00:00', 'BERTHED');

-- ------------------------------------------------------------------------
-- Seed Data: Yard Slots across Blocks A, B, C
-- ------------------------------------------------------------------------
INSERT INTO yard_slots (slot_id, block_name, tier_level, slot_type, is_occupied, assigned_crane_id) VALUES
('BLK-A-R01-T1', 'Block A (Reefer Zone)', 1, 'REEFER', FALSE, 'CRANE-A1'),
('BLK-A-R01-T2', 'Block A (Reefer Zone)', 2, 'REEFER', FALSE, 'CRANE-A1'),
('BLK-A-R02-T1', 'Block A (Reefer Zone)', 1, 'REEFER', FALSE, 'CRANE-A2'),
('BLK-A-R02-T2', 'Block A (Reefer Zone)', 2, 'REEFER', FALSE, 'CRANE-A2'),
('BLK-B-D01-T1', 'Block B (Dry Cargo Zone)', 1, 'STANDARD_DRY', FALSE, 'CRANE-B1'),
('BLK-B-D01-T2', 'Block B (Dry Cargo Zone)', 2, 'STANDARD_DRY', FALSE, 'CRANE-B1'),
('BLK-B-D02-T1', 'Block B (Dry Cargo Zone)', 1, 'STANDARD_DRY', FALSE, 'CRANE-B2'),
('BLK-B-D02-T2', 'Block B (Dry Cargo Zone)', 2, 'STANDARD_DRY', FALSE, 'CRANE-B2'),
('BLK-C-H01-T1', 'Block C (Hazmat Safe Zone)', 1, 'HAZMAT', FALSE, 'CRANE-C1'),
('BLK-C-H01-T2', 'Block C (Hazmat Safe Zone)', 2, 'HAZMAT', FALSE, 'CRANE-C1'),
('BLK-C-H02-T1', 'Block C (Hazmat Safe Zone)', 1, 'HAZMAT', FALSE, 'CRANE-C2'),
('BLK-C-H02-T2', 'Block C (Hazmat Safe Zone)', 2, 'HAZMAT', FALSE, 'CRANE-C2');
