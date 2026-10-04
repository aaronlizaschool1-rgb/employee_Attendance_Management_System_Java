/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.attendance.dao;

import com.attendance.model.LeaveRecord;
import com.attendance.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Aaron
 */



public class LeaveDAO {

    
     //files a new leave request into the database - check contraint in the schema and use input validation in business logic/ui layer
    
     
    public boolean fileLeave(LeaveRecord leave) throws SQLException {
        String sql = "INSERT INTO leave_records (employee_id, start_date, end_date, leave_type, status, reason, reviewed_by) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, leave.getEmployeeId());
            ps.setDate(2, Date.valueOf(leave.getStartDate()));
            ps.setDate(3, Date.valueOf(leave.getEndDate()));
            ps.setString(4, leave.getLeaveType());
            ps.setString(5, leave.getStatus() != null ? leave.getStatus() : "PENDING");
            ps.setString(6, leave.getReason());

            if (leave.getReviewedBy() != null && leave.getReviewedBy() > 0) {
                ps.setInt(7, leave.getReviewedBy());
            } else {
                ps.setNull(7, Types.INTEGER);
            }

            return ps.executeUpdate() > 0;
        }
    }

    
     //Updates the status of an existing leave request (APPROVED / REJECTED)
    
    public boolean updateStatus(int leaveId, String status, int reviewerId) throws SQLException {
        String sql = "UPDATE leave_records SET status = ?, reviewed_by = ? WHERE leave_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, status);
            ps.setInt(2, reviewerId);
            ps.setInt(3, leaveId);

            return ps.executeUpdate() > 0;
        }
    }

    
     // Retrieves all pending leave requests waiting for manager or admin action
    
    public List<LeaveRecord> getPendingLeaves() throws SQLException {
        List<LeaveRecord> list = new ArrayList<>();
        String sql = "SELECT leave_id, employee_id, start_date, end_date, leave_type, status, reason, reviewed_by "
                   + "FROM leave_records WHERE status = 'PENDING' ORDER BY start_date ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapResultSetToLeaveRecord(rs));
            }
        }
        return list;
    }

    
     //Retrieves all leave filings submitted by a specific employee
     
    public List<LeaveRecord> getLeavesByEmployee(int employeeId) throws SQLException {
        List<LeaveRecord> list = new ArrayList<>();
        String sql = "SELECT leave_id, employee_id, start_date, end_date, leave_type, status, reason, reviewed_by "
                   + "FROM leave_records WHERE employee_id = ? ORDER BY start_date DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, employeeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToLeaveRecord(rs));
                }
            }
        }
        return list;
    }

    
     //Retrieves all leave records with joined details (employee name, department, and reviewer name) - if di gets ask me sa table relationship
     
    public List<Object[]> getAllLeavesWithDetails() throws SQLException {
        List<Object[]> rows = new ArrayList<>();
        String sql = "SELECT l.leave_id, l.employee_id, "
                   + "       e.first_name || ' ' || e.last_name AS employee_name, "
                   + "       d.department_name, "
                   + "       l.start_date, l.end_date, l.leave_type, l.status, l.reason, "
                   + "       u.full_name AS reviewer_name "
                   + "FROM leave_records l "
                   + "INNER JOIN employees e ON l.employee_id = e.employee_id "
                   + "INNER JOIN departments d ON e.department_id = d.department_id "
                   + "LEFT OUTER JOIN users u ON l.reviewed_by = u.user_id "
                   + "ORDER BY l.leave_id DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String reviewer = rs.getString("reviewer_name");
                rows.add(new Object[] {
                    rs.getInt("leave_id"),
                    rs.getInt("employee_id"),
                    rs.getString("employee_name"),
                    rs.getString("department_name"),
                    rs.getDate("start_date").toString(),
                    rs.getDate("end_date").toString(),
                    rs.getString("leave_type"),
                    rs.getString("status"),
                    rs.getString("reason") != null ? rs.getString("reason") : "",
                    reviewer != null ? reviewer : "Unreviewed"
                });
            }
        }
        return rows;
    }

    
     // Counts the total number of pending leave applications.
    
    public int getPendingLeaveCount() throws SQLException {
        String sql = "SELECT COUNT(*) FROM leave_records WHERE status = 'PENDING'";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    
     // Deletes a leave request by ID.
   
    public boolean deleteLeave(int leaveId) throws SQLException {
        String sql = "DELETE FROM leave_records WHERE leave_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, leaveId);
            return ps.executeUpdate() > 0;
        }
    }

    
     //Maps a single ResultSet cursor row to a LeaveRecord instance.
     
    private LeaveRecord mapResultSetToLeaveRecord(ResultSet rs) throws SQLException {
        LeaveRecord record = new LeaveRecord();
        record.setLeaveId(rs.getInt("leave_id"));
        record.setEmployeeId(rs.getInt("employee_id"));
        record.setStartDate(rs.getDate("start_date").toString());
        record.setEndDate(rs.getDate("end_date").toString());
        record.setLeaveType(rs.getString("leave_type"));
        record.setStatus(rs.getString("status"));
        record.setReason(rs.getString("reason"));

        int reviewerId = rs.getInt("reviewed_by");
        if (rs.wasNull()) {
            record.setReviewedBy(null);
        } else {
            record.setReviewedBy(reviewerId);
        }

        return record;
    }
}