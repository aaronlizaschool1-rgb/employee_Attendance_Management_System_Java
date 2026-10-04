CREATE TABLE users (
    user_id INT GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
    username VARCHAR(50) NOT NULL,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    CONSTRAINT pk_users PRIMARY KEY (user_id),
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT chk_user_role CHECK (role IN ('ADMINISTRATOR', 'EMPLOYEE', 'MANAGER')),
    CONSTRAINT chk_user_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE TABLE departments (
    department_id INT GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
    department_name VARCHAR(60) NOT NULL,
    CONSTRAINT pk_departments PRIMARY KEY (department_id),
    CONSTRAINT uk_dept_name UNIQUE (department_name)
);

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
        REFERENCES users(user_id) ON DELETE SET NULL
);