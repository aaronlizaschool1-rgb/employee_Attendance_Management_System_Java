/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.attendance.dao;

/**
 *
 * @author Aaron
 */
import com.attendance.model.Employee;
import com.attendance.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.util.ArrayList;
import java.util.List;


public class EmployeeDAO {

   
     //CREATE: Inserts a new employee record into the database[cite: 22, 25].
    
    public boolean addEmployee(Employee emp) throws SQLException {
        String sql = "INSERT INTO employees (first_name, last_name, email, department_id, "
                   + "job_title, shift_start, shift_end, status) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, emp.getFirstName());
            ps.setString(2, emp.getLastName());
            ps.setString(3, emp.getEmail());
            ps.setInt(4, emp.getDepartmentId());
            ps.setString(5, emp.getJobTitle());
            ps.setTime(6, Time.valueOf(emp.getShiftStart()));
            ps.setTime(7, Time.valueOf(emp.getShiftEnd()));
            ps.setString(8, emp.getStatus());

            return ps.executeUpdate() > 0;
        }
    }

   
     //READ: Retrieves all employee records ordered by employee ID[cite: 22, 25].
  
    public List<Employee> getAllEmployees() throws SQLException {
        List<Employee> list = new ArrayList<>();
        String sql = "SELECT employee_id, first_name, last_name, email, department_id, "
                   + "job_title, shift_start, shift_end, status "
                   + "FROM employees ORDER BY employee_id ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapResultSetToEmployee(rs));
            }
        }
        return list;
    }

    
     //READ: Finds a specific employee by ID[cite: 22, 25].
     
    public Employee getEmployeeById(int employeeId) throws SQLException {
        String sql = "SELECT employee_id, first_name, last_name, email, department_id, "
                   + "job_title, shift_start, shift_end, status "
                   + "FROM employees WHERE employee_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, employeeId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToEmployee(rs);
                }
            }
        }
        return null;
    }

    
     //UPDATE: Updates an existing employee profile[cite: 22, 25].
     
    public boolean updateEmployee(Employee emp) throws SQLException {
        String sql = "UPDATE employees SET first_name = ?, last_name = ?, email = ?, "
                   + "department_id = ?, job_title = ?, shift_start = ?, shift_end = ?, status = ? "
                   + "WHERE employee_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, emp.getFirstName());
            ps.setString(2, emp.getLastName());
            ps.setString(3, emp.getEmail());
            ps.setInt(4, emp.getDepartmentId());
            ps.setString(5, emp.getJobTitle());
            ps.setTime(6, Time.valueOf(emp.getShiftStart()));
            ps.setTime(7, Time.valueOf(emp.getShiftEnd()));
            ps.setString(8, emp.getStatus());
            ps.setInt(9, emp.getEmployeeId());

            return ps.executeUpdate() > 0;
        }
    }

    
      //DELETE: Removes an employee by ID[cite: 22, 25].
     
    public boolean deleteEmployee(int employeeId) throws SQLException {
        String sql = "DELETE FROM employees WHERE employee_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, employeeId);
            return ps.executeUpdate() > 0;
        }
    }

     //READ: Filters records dynamically by matching a keyword across:
     
    public List<Employee> searchEmployees(String keyword) throws SQLException {
        List<Employee> list = new ArrayList<>();
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllEmployees();
        }

        String trimmed = keyword.trim();
        boolean isNumeric = trimmed.matches("\\d+");

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT employee_id, first_name, last_name, email, department_id, ")
           .append("job_title, shift_start, shift_end, status ")
           .append("FROM employees ")
           .append("WHERE LOWER(first_name) LIKE ? ")
           .append("   OR LOWER(last_name) LIKE ? ")
           .append("   OR LOWER(job_title) LIKE ? ");

        if (isNumeric) {
            sql.append("   OR employee_id = ? ");
        }

        sql.append("ORDER BY employee_id ASC");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            String wildcardPattern = "%" + trimmed.toLowerCase() + "%";
            ps.setString(1, wildcardPattern);
            ps.setString(2, wildcardPattern);
            ps.setString(3, wildcardPattern);

            if (isNumeric) {
                ps.setInt(4, Integer.parseInt(trimmed));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToEmployee(rs));
                }
            }
        }
        return list;
    }

    
      //JOIN: Retrieves all employees with their resolved department names
      
    public List<Object[]> getEmployeesWithDepartment() throws SQLException {
        List<Object[]> rows = new ArrayList<>();
        String sql = "SELECT e.employee_id, e.first_name, e.last_name, e.email, "
                   + "d.department_name, e.job_title, e.shift_start, e.shift_end, e.status "
                   + "FROM employees e "
                   + "INNER JOIN departments d ON e.department_id = d.department_id "
                   + "ORDER BY e.employee_id ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                rows.add(new Object[] {
                    rs.getInt("employee_id"),
                    rs.getString("first_name"),
                    rs.getString("last_name"),
                    rs.getString("email"),
                    rs.getString("department_name"),
                    rs.getString("job_title"),
                    rs.getTime("shift_start").toString(),
                    rs.getTime("shift_end").toString(),
                    rs.getString("status")
                });
            }
        }
        return rows;
    }

   
     //Helper method to map a ResultSet cursor row to an Employee model instance
    
    private Employee mapResultSetToEmployee(ResultSet rs) throws SQLException {
        Employee emp = new Employee();
        emp.setEmployeeId(rs.getInt("employee_id"));
        emp.setFirstName(rs.getString("first_name"));
        emp.setLastName(rs.getString("last_name"));
        emp.setEmail(rs.getString("email"));
        emp.setDepartmentId(rs.getInt("department_id"));
        emp.setJobTitle(rs.getString("job_title"));
        emp.setShiftStart(rs.getTime("shift_start").toString());
        emp.setShiftEnd(rs.getTime("shift_end").toString());
        emp.setStatus(rs.getString("status"));
        return emp;
    }
}