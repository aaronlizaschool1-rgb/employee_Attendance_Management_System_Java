-- =============================================================================
-- EMPLOYEE ATTENDANCE MANAGEMENT SYSTEM (EAMS)
-- File Target    : src/main/resources/db/migration/V2__derby_seed.sql
-- RDBMS Engine   : Apache Derby (Embedded / Network)
-- Description    : Revised seed script matching updated check constraints
--                  (role: ADMINISTRATOR, EMPLOYEE, MANAGER) with at least 10
--                  records per table for UI and query testing.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. SEED DATA: USERS (10 Records)
-- Validated against: CHECK (role IN ('ADMINISTRATOR', 'EMPLOYEE', 'MANAGER'))
-- -----------------------------------------------------------------------------
INSERT INTO users (username, password, full_name, role, status) VALUES
('admin', 'admin123', 'System Administrator', 'ADMINISTRATOR', 'ACTIVE'),
('aliza', 'pass123', 'Aaron Liza', 'ADMINISTRATOR', 'ACTIVE'),
('mgarcia', 'pass123', 'Maria Garcia', 'MANAGER', 'ACTIVE'),
('jdelacruz', 'pass123', 'Juan Dela Cruz', 'MANAGER', 'ACTIVE'),
('cramirez', 'pass123', 'Chloe Ramirez', 'EMPLOYEE', 'ACTIVE'),
('rreyes', 'pass123', 'Rafael Reyes', 'EMPLOYEE', 'ACTIVE'),
('bsantos', 'pass123', 'Beatriz Santos', 'EMPLOYEE', 'ACTIVE'),
('cfernando', 'pass123', 'Carlo Fernando', 'EMPLOYEE', 'ACTIVE'),
('ltan', 'pass123', 'Leah Tan', 'EMPLOYEE', 'ACTIVE'),
('jramos_inact', 'pass123', 'Jerome Ramos', 'EMPLOYEE', 'INACTIVE');

-- -----------------------------------------------------------------------------
-- 2. SEED DATA: DEPARTMENTS (10 Records)
-- Generates department_id values 1 to 10
-- -----------------------------------------------------------------------------
INSERT INTO departments (department_name) VALUES
('Human Resources'),
('Information Technology'),
('Finance and Accounting'),
('Operations and Logistics'),
('Sales and Marketing'),
('Customer Support'),
('Legal and Compliance'),
('Research and Development'),
('Quality Assurance'),
('Executive Management');

-- -----------------------------------------------------------------------------
-- 3. SEED DATA: EMPLOYEES (12 Records)
-- Foreign Keys: department_id (1-10)
-- Validated against: CHECK (status IN ('ACTIVE', 'INACTIVE', 'TERMINATED'))
-- -----------------------------------------------------------------------------
INSERT INTO employees (first_name, last_name, email, department_id, job_title, shift_start, shift_end, status) VALUES
('Aaron', 'Liza', 'aaron.liza@attendance.com', 2, 'Lead Systems Architect', TIME('09:00:00'), TIME('18:00:00'), 'ACTIVE'),
('Maria', 'Garcia', 'maria.garcia@attendance.com', 1, 'HR Director', TIME('08:30:00'), TIME('17:30:00'), 'ACTIVE'),
('Juan', 'Dela Cruz', 'juan.delacruz@attendance.com', 4, 'Operations Manager', TIME('08:00:00'), TIME('17:00:00'), 'ACTIVE'),
('Chloe', 'Ramirez', 'chloe.ramirez@attendance.com', 6, 'Front Desk Associate', TIME('09:00:00'), TIME('18:00:00'), 'ACTIVE'),
('Rafael', 'Reyes', 'rafael.reyes@attendance.com', 2, 'Full Stack Developer', TIME('09:00:00'), TIME('18:00:00'), 'ACTIVE'),
('Beatriz', 'Santos', 'beatriz.santos@attendance.com', 3, 'Senior Accountant', TIME('08:30:00'), TIME('17:30:00'), 'ACTIVE'),
('Carlo', 'Fernando', 'carlo.fernando@attendance.com', 5, 'Marketing Specialist', TIME('09:00:00'), TIME('18:00:00'), 'ACTIVE'),
('Leah', 'Tan', 'leah.tan@attendance.com', 8, 'Data Analyst', TIME('09:00:00'), TIME('18:00:00'), 'ACTIVE'),
('Dominic', 'Roxas', 'dominic.roxas@attendance.com', 9, 'QA Automation Engineer', TIME('09:00:00'), TIME('18:00:00'), 'ACTIVE'),
('Sofia', 'Mercado', 'sofia.mercado@attendance.com', 7, 'Corporate Legal Counsel', TIME('08:30:00'), TIME('17:30:00'), 'ACTIVE'),
('Victor', 'Aquino', 'victor.aquino@attendance.com', 10, 'Managing Director', TIME('08:00:00'), TIME('17:00:00'), 'ACTIVE'),
('Jerome', 'Ramos', 'jerome.ramos@attendance.com', 4, 'Warehouse Coordinator', TIME('08:00:00'), TIME('17:00:00'), 'TERMINATED');

-- -----------------------------------------------------------------------------
-- 4. SEED DATA: ATTENDANCE_LOGS (12 Records)
-- Enforces: UNIQUE (employee_id, log_date)
-- Validated against: CHECK (status IN ('ON_TIME', 'LATE', 'HALF_DAY', 'UNDERTIME'))
-- -----------------------------------------------------------------------------
INSERT INTO attendance_logs (employee_id, log_date, time_in, time_out, status) VALUES
(1, DATE('2026-10-01'), TIME('08:55:00'), TIME('18:05:00'), 'ON_TIME'),
(2, DATE('2026-10-01'), TIME('08:28:00'), TIME('17:32:00'), 'ON_TIME'),
(3, DATE('2026-10-01'), TIME('08:25:00'), TIME('17:00:00'), 'LATE'),
(4, DATE('2026-10-01'), TIME('09:02:00'), TIME('18:00:00'), 'ON_TIME'),
(5, DATE('2026-10-01'), TIME('09:35:00'), TIME('18:30:00'), 'LATE'),
(6, DATE('2026-10-01'), TIME('08:30:00'), TIME('17:35:00'), 'ON_TIME'),
(7, DATE('2026-10-01'), TIME('09:00:00'), TIME('15:00:00'), 'UNDERTIME'),
(8, DATE('2026-10-01'), TIME('09:12:00'), TIME('13:15:00'), 'HALF_DAY'),
(9, DATE('2026-10-01'), TIME('08:58:00'), TIME('18:02:00'), 'ON_TIME'),
(10, DATE('2026-10-01'), TIME('08:29:00'), TIME('17:30:00'), 'ON_TIME'),
(1, DATE('2026-10-02'), TIME('08:52:00'), TIME('18:10:00'), 'ON_TIME'),
(5, DATE('2026-10-02'), TIME('08:57:00'), TIME('18:00:00'), 'ON_TIME');

-- -----------------------------------------------------------------------------
-- 5. SEED DATA: LEAVE_RECORDS (10 Records)
-- Foreign Keys: employee_id (1-10), reviewed_by references users(user_id) (1-4)
-- Validated against: CHECK (leave_type IN ('SICK', 'VACATION', 'EMERGENCY', 'UNPAID'))
-- Validated against: CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'))
-- -----------------------------------------------------------------------------
INSERT INTO leave_records (employee_id, start_date, end_date, leave_type, status, reason, reviewed_by) VALUES
(1, DATE('2026-10-15'), DATE('2026-10-17'), 'VACATION', 'APPROVED', 'Annual scheduled family vacation', 1),
(2, DATE('2026-10-20'), DATE('2026-10-21'), 'VACATION', 'PENDING', 'Personal time off', NULL),
(3, DATE('2026-10-05'), DATE('2026-10-06'), 'EMERGENCY', 'APPROVED', 'Residential water pipe emergency repair', 1),
(4, DATE('2026-09-18'), DATE('2026-09-19'), 'SICK', 'APPROVED', 'Seasonal flu symptoms with medical certificate', 2),
(5, DATE('2026-11-02'), DATE('2026-11-06'), 'VACATION', 'PENDING', 'Attending regional developer conference', NULL),
(6, DATE('2026-10-12'), DATE('2026-10-12'), 'EMERGENCY', 'REJECTED', 'Late filing request without prior notice', 3),
(7, DATE('2026-10-25'), DATE('2026-10-26'), 'UNPAID', 'PENDING', 'Extended personal rest leave', NULL),
(8, DATE('2026-09-01'), DATE('2026-09-02'), 'SICK', 'APPROVED', 'Severe migraine recovery', 2),
(9, DATE('2026-10-08'), DATE('2026-10-09'), 'VACATION', 'APPROVED', 'Long weekend schedule', 1),
(10, DATE('2026-10-30'), DATE('2026-10-31'), 'EMERGENCY', 'PENDING', 'Family matter attendance', NULL);