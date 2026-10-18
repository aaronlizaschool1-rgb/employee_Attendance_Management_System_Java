-- =============================================================================
-- EMPLOYEE ATTENDANCE MANAGEMENT SYSTEM (EAMS)
-- File Target    : src/main/resources/db/migration/V1__derby_schema.sql
-- RDBMS Engine   : Apache Derby (Embedded / Network)
-- Description    : Safe teardown (DROP IF EXISTS) and complete 3NF schema 
--                  with Employee-User 1:1 cascade and status sync triggers.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- TEARDOWN / CLEANUP (Reverse dependency order to avoid foreign key violations)
-- -----------------------------------------------------------------------------

-- -----------------------------------------------------------------------------
-- 1. DEPARTMENTS
-- -----------------------------------------------------------------------------
CREATE TABLE departments (
    department_id INT GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
    department_name VARCHAR(60) NOT NULL,
    CONSTRAINT pk_departments PRIMARY KEY (department_id),
    CONSTRAINT uk_dept_name UNIQUE (department_name)
);

-- -----------------------------------------------------------------------------
-- 2. EMPLOYEES
-- Master table for workforce personnel profiles
-- -----------------------------------------------------------------------------
CREATE TABLE employees (
    employee_id INT GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL,
    department_id INT NOT NULL,
    job_title VARCHAR(50) NOT NULL,
    shift_start TIME NOT NULL,
    shift_end TIME NOT NULL,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    CONSTRAINT pk_employees PRIMARY KEY (employee_id),
    CONSTRAINT uk_employee_email UNIQUE (email),
    CONSTRAINT chk_emp_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'TERMINATED')),
    CONSTRAINT fk_emp_dept FOREIGN KEY (department_id) 
        REFERENCES departments(department_id) ON DELETE RESTRICT
);

-- -----------------------------------------------------------------------------
-- 3. USERS
-- Linked to employees (1:1 mapping with ON DELETE CASCADE)
-- employee_id is nullable to permit independent root administrator accounts
-- -----------------------------------------------------------------------------
CREATE TABLE users (
    user_id INT GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
    employee_id INT,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    CONSTRAINT pk_users PRIMARY KEY (user_id),
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_employee UNIQUE (employee_id),
    CONSTRAINT chk_user_role CHECK (role IN ('ADMINISTRATOR', 'EMPLOYEE', 'MANAGER')),
    CONSTRAINT chk_user_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT fk_user_employee FOREIGN KEY (employee_id) 
        REFERENCES employees(employee_id) ON DELETE CASCADE
);

-- -----------------------------------------------------------------------------
-- 4. ATTENDANCE_LOGS
-- Cascades when employee is removed
-- -----------------------------------------------------------------------------
CREATE TABLE attendance_logs (
    log_id INT GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
    employee_id INT NOT NULL,
    log_date DATE NOT NULL,
    time_in TIME,
    time_out TIME,
    status VARCHAR(20) DEFAULT 'ON_TIME',
    CONSTRAINT pk_attendance PRIMARY KEY (log_id),
    CONSTRAINT uk_emp_log_date UNIQUE (employee_id, log_date),
    CONSTRAINT chk_att_status CHECK (status IN ('ON_TIME', 'LATE', 'HALF_DAY', 'UNDERTIME')),
    CONSTRAINT fk_att_employee FOREIGN KEY (employee_id) 
        REFERENCES employees(employee_id) ON DELETE CASCADE
);

-- -----------------------------------------------------------------------------
-- 5. LEAVE_RECORDS
-- Cascades when employee is removed; sets reviewer to NULL if user is removed
-- -----------------------------------------------------------------------------
-- =============================================================================
-- 5. LEAVE_RECORDS
-- Both foreign keys use ON DELETE CASCADE to satisfy Derby's delete-path rule
-- =============================================================================
CREATE TABLE leave_records (
    leave_id INT GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
    employee_id INT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    leave_type VARCHAR(30) NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING',
    reason VARCHAR(255),
    reviewed_by INT,
    CONSTRAINT pk_leaves PRIMARY KEY (leave_id),
    CONSTRAINT chk_leave_type CHECK (leave_type IN ('SICK', 'VACATION', 'EMERGENCY', 'UNPAID')),
    CONSTRAINT chk_leave_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    CONSTRAINT fk_leave_employee FOREIGN KEY (employee_id) 
        REFERENCES employees(employee_id) ON DELETE CASCADE,
    CONSTRAINT fk_leave_reviewer FOREIGN KEY (reviewed_by) 
        REFERENCES users(user_id) ON DELETE CASCADE
);

-- -----------------------------------------------------------------------------
-- 6. STATUS SYNCHRONIZATION TRIGGERS
-- Automatically deactivates or reactivates user account on employee status update
-- -----------------------------------------------------------------------------
CREATE TRIGGER trg_sync_emp_inactive_user
AFTER UPDATE OF status ON employees
REFERENCING NEW TABLE AS updated_emps
FOR EACH STATEMENT
    UPDATE users
    SET status = 'INACTIVE'
    WHERE employee_id IN (
        SELECT employee_id 
        FROM updated_emps 
        WHERE status IN ('INACTIVE', 'TERMINATED')
    );

CREATE TRIGGER trg_sync_emp_active_user
AFTER UPDATE OF status ON employees
REFERENCING NEW TABLE AS updated_emps
FOR EACH STATEMENT
    UPDATE users
    SET status = 'ACTIVE'
    WHERE employee_id IN (
        SELECT employee_id 
        FROM updated_emps 
        WHERE status = 'ACTIVE'
    );