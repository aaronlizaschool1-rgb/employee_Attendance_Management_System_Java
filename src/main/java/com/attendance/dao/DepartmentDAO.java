/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.attendance.dao;
import com.attendance.model.Department;
import com.attendance.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
/**
 *
 * @author Aaron
 */
public class DepartmentDAO {

    
     // CREATE: Inserts a new department record
     
    public boolean addDepartment(Department department) throws SQLException {
        String sql = "INSERT INTO departments (department_name) VALUES (?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, department.getDepartmentName());
            return ps.executeUpdate() > 0;
        }
    }

    
    //READ: Retrieves all departments sorted alphabetically
     
    public List<Department> getAllDepartments() throws SQLException {
        List<Department> list = new ArrayList<>();
        String sql = "SELECT department_id, department_name FROM departments ORDER BY department_name ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Department dept = new Department();
                dept.setDepartmentId(rs.getInt("department_id"));
                dept.setDepartmentName(rs.getString("department_name"));
                list.add(dept);
            }
        }
        return list;
    }

    
     //UPDATE: Modifies an existing department's name
     
    public boolean updateDepartment(Department department) throws SQLException {
        String sql = "UPDATE departments SET department_name = ? WHERE department_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, department.getDepartmentName());
            ps.setInt(2, department.getDepartmentId());
            return ps.executeUpdate() > 0;
        }
    }

  
     //DELETE: Removes a department by ID (fails if employees are assigned due to ON DELETE RESTRICT)
    // NOTE - LAGYAN NG BUSINESS LOGIC SA UI PART FOR ERROR HANDLING
     
    public boolean deleteDepartment(int departmentId) throws SQLException {
        String sql = "DELETE FROM departments WHERE department_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, departmentId);
            return ps.executeUpdate() > 0;
        }
    }

    // READ ID - SEARCH FUNCTION
    public Department getDepartmentById(int departmentId) throws SQLException {
        String sql = "SELECT department_id, department_name FROM departments WHERE department_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, departmentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Department(
                        rs.getInt("department_id"),
                        rs.getString("department_name")
                    );
                }
            }
        }
        return null;
    }
}