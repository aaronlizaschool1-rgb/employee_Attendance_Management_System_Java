/* =============================================================================
   EMPLOYEE ATTENDANCE MANAGEMENT SYSTEM (EAMS)
   Target Database : MySQL 8.0+ / MariaDB 10.5+
   Normalization   : Strict Third Normal Form (3NF)
   Standards       : ISO SQL / ANSI, Explicit Junction Keys (No Synthetic Auto-Inc),
                     Zero Transitive Dependencies, Engine-Enforced Referential Integrity
   File Target     : src/main/resources/db/migration/V1__schema.sql
   ============================================================================= */

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE DATABASE IF NOT EXISTS `attendance_db`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE `attendance_db`;

-- Drop objects in strict reverse-dependency order
DROP VIEW IF EXISTS `vw_monthly_attendance_report`;
DROP VIEW IF EXISTS `vw_daily_attendance_summary`;

DROP TABLE IF EXISTS `employee_departments`;
DROP TABLE IF EXISTS `audit_logs`;
DROP TABLE IF EXISTS `leave_records`;
DROP TABLE IF EXISTS `attendance_logs`;
DROP TABLE IF EXISTS `employees`;
DROP TABLE IF EXISTS `departments`;

/* =============================================================================
   1. TABLE: departments
   Atomic entity storing structural organizational units.
   ============================================================================= */
CREATE TABLE `departments` (
    `department_id` INT NOT NULL AUTO_INCREMENT,
    `department_name` VARCHAR(60) NOT NULL,
    PRIMARY KEY (`department_id`),
    UNIQUE KEY `uk_department_name` (`department_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

/* =============================================================================
   2. TABLE: employees
   Master record containing personal profile, credential email, access role,
   and standard operational schedule.
   ============================================================================= */
CREATE TABLE `employees` (
    `employee_id` INT NOT NULL AUTO_INCREMENT,
    `first_name` VARCHAR(50) NOT NULL,
    `last_name` VARCHAR(50) NOT NULL,
    `email` VARCHAR(100) NOT NULL,
    `job_title` VARCHAR(50) NOT NULL,
    `system_role` ENUM('ADMIN', 'HR', 'EMPLOYEE') NOT NULL DEFAULT 'EMPLOYEE',
    `shift_start` TIME NOT NULL DEFAULT '09:00:00',
    `shift_end` TIME NOT NULL DEFAULT '18:00:00',
    `status` ENUM('ACTIVE', 'INACTIVE', 'TERMINATED') NOT NULL DEFAULT 'ACTIVE',
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`employee_id`),
    UNIQUE KEY `uk_employee_email` (`email`),
    INDEX `idx_employee_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

/* =============================================================================
   3. TABLE: employee_departments (Pure 3NF Junction / Bridge Table)
   - Resolves Employee <-> Department mapping cleanly.
   - STANDARD COMPLIANT: NO redundant AUTO_INCREMENT surrogate column.
   - Uses a Composite Natural Primary Key (`employee_id`, `department_id`)
     to guarantee a person cannot be assigned to the same department twice.
   ============================================================================= */
CREATE TABLE `employee_departments` (
    `employee_id` INT NOT NULL,
    `department_id` INT NOT NULL,
    `assigned_at` DATE NOT NULL DEFAULT (CURRENT_DATE),
    PRIMARY KEY (`employee_id`, `department_id`),
    INDEX `idx_bridge_dept` (`department_id`),
    CONSTRAINT `fk_bridge_employee`
        FOREIGN KEY (`employee_id`)
        REFERENCES `employees` (`employee_id`)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT `fk_bridge_department`
        FOREIGN KEY (`department_id`)
        REFERENCES `departments` (`department_id`)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

/* =============================================================================
   4. TABLE: attendance_logs
   - Natural transaction log: Stores raw facts (time_in, time_out).
   - Strict 3NF: No pre-computed late/undertime values stored to prevent
     transitive update anomalies. Calculations belong in reporting views.
   - Business constraint: One attendance entry per employee per work date.
   ============================================================================= */
CREATE TABLE `attendance_logs` (
    `log_id` INT NOT NULL AUTO_INCREMENT,
    `employee_id` INT NOT NULL,
    `log_date` DATE NOT NULL,
    `time_in` TIME NULL,
    `time_out` TIME NULL,
    `status` ENUM('ON_TIME', 'LATE', 'HALF_DAY', 'UNDERTIME') NOT NULL DEFAULT 'ON_TIME',
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`log_id`),
    UNIQUE KEY `uk_employee_log_date` (`employee_id`, `log_date`),
    INDEX `idx_log_date` (`log_date`),
    INDEX `idx_attendance_status` (`status`),
    CONSTRAINT `fk_attendance_employee`
        FOREIGN KEY (`employee_id`)
        REFERENCES `employees` (`employee_id`)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

/* =============================================================================
   5. TABLE: leave_records
   - Absence requests with date ranges and review workflow tracking.
   - Self-referencing FK (`reviewed_by`) targets `employees.employee_id`.
   ============================================================================= */
CREATE TABLE `leave_records` (
    `leave_id` INT NOT NULL AUTO_INCREMENT,
    `employee_id` INT NOT NULL,
    `start_date` DATE NOT NULL,
    `end_date` DATE NOT NULL,
    `leave_type` ENUM('SICK', 'VACATION', 'EMERGENCY', 'MATERNITY', 'PATERNITY', 'UNPAID') NOT NULL,
    `status` ENUM('PENDING', 'APPROVED', 'REJECTED') NOT NULL DEFAULT 'PENDING',
    `reason` VARCHAR(255) NULL,
    `reviewed_by` INT NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`leave_id`),
    INDEX `idx_leave_dates` (`start_date`, `end_date`),
    INDEX `idx_leave_status` (`status`),
    CONSTRAINT `fk_leave_employee`
        FOREIGN KEY (`employee_id`)
        REFERENCES `employees` (`employee_id`)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT `fk_leave_reviewer`
        FOREIGN KEY (`reviewed_by`)
        REFERENCES `employees` (`employee_id`)
        ON DELETE SET NULL
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

/* =============================================================================
   6. TABLE: audit_logs
   Independent security ledger for transaction changes.
   ============================================================================= */
CREATE TABLE `audit_logs` (
    `audit_id` BIGINT NOT NULL AUTO_INCREMENT,
    `employee_id` INT NULL,
    `action` VARCHAR(50) NOT NULL,
    `details` TEXT NULL,
    `ip_address` VARCHAR(45) NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`audit_id`),
    INDEX `idx_audit_timestamp` (`created_at`),
    CONSTRAINT `fk_audit_employee`
        FOREIGN KEY (`employee_id`)
        REFERENCES `employees` (`employee_id`)
        ON DELETE SET NULL
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

/* =============================================================================
   7. VIEW: vw_daily_attendance_summary
   Computes derived metrics (Late, Undertime, Overtime) dynamically at query time
   without violating 3NF storage rules.
   ============================================================================= */
CREATE OR REPLACE VIEW `vw_daily_attendance_summary` AS
SELECT 
    a.log_id,
    a.employee_id,
    CONCAT(e.first_name, ' ', e.last_name) AS full_name,
    d.department_name,
    a.log_date,
    a.time_in,
    a.time_out,
    e.shift_start,
    e.shift_end,
    -- Compute Late Minutes (positive difference between time_in and shift_start)
    CASE 
        WHEN a.time_in IS NOT NULL AND a.time_in > e.shift_start 
        THEN TIMESTAMPDIFF(MINUTE, e.shift_start, a.time_in)
        ELSE 0 
    END AS calculated_late_minutes,
    -- Compute Undertime Minutes (early leave prior to shift_end)
    CASE 
        WHEN a.time_out IS NOT NULL AND a.time_out < e.shift_end 
        THEN TIMESTAMPDIFF(MINUTE, a.time_out, e.shift_end)
        ELSE 0 
    END AS calculated_undertime_minutes,
    -- Compute Overtime Minutes (work past shift_end)
    CASE 
        WHEN a.time_out IS NOT NULL AND a.time_out > e.shift_end 
        THEN TIMESTAMPDIFF(MINUTE, e.shift_end, a.time_out)
        ELSE 0 
    END AS calculated_overtime_minutes,
    a.status
FROM attendance_logs a
JOIN employees e ON a.employee_id = e.employee_id
LEFT JOIN employee_departments ed ON e.employee_id = ed.employee_id
LEFT JOIN departments d ON ed.department_id = d.department_id;

SET FOREIGN_KEY_CHECKS = 1;