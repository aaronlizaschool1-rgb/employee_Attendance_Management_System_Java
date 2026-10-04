/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package com.attendance.dao;

import com.attendance.model.AttendanceLog;
import com.attendance.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.util.ArrayList;
import java.util.List;


/**
 *
 * @author Aaron
 */


public class AttendanceDAO {

    
      //Checks if an employee has already clocked in on a specified date - TRUE.
     
    public boolean hasClockedInToday(int employeeId, String logDate) throws SQLException {
        String sql = "SELECT COUNT(*) FROM attendance_logs WHERE employee_id = ? AND log_date = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, employeeId);
            ps.setDate(2, Date.valueOf(logDate));

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    
     //Inserts a clock-in record for an employee - BUSINESS LOGIC FOR THE INPUT VALIDATION: UI SIDE.
     
     
    public boolean recordClockIn(int employeeId, String logDate, String timeIn, String status) throws SQLException {
        String sql = "INSERT INTO attendance_logs (employee_id, log_date, time_in, status) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, employeeId);
            ps.setDate(2, Date.valueOf(logDate));
            ps.setTime(3, Time.valueOf(timeIn));
            ps.setString(4, status);

            return ps.executeUpdate() > 0;
        }
    }

    
     //Updates an existing attendance log with a clock-out timestamp
     
  
    public boolean recordClockOut(int employeeId, String logDate, String timeOut) throws SQLException {
        String sql = "UPDATE attendance_logs SET time_out = ? WHERE employee_id = ? AND log_date = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setTime(1, Time.valueOf(timeOut));
            ps.setInt(2, employeeId);
            ps.setDate(3, Date.valueOf(logDate));

            return ps.executeUpdate() > 0;
        }
    }

    
     //Retrieves all attendance logs for a specific employee ordered by date descending
    
    public List<AttendanceLog> getLogsByEmployee(int employeeId) throws SQLException {
        List<AttendanceLog> list = new ArrayList<>();
        String sql = "SELECT log_id, employee_id, log_date, time_in, time_out, status "
                   + "FROM attendance_logs WHERE employee_id = ? ORDER BY log_date DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, employeeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToAttendanceLog(rs));
                }
            }
        }
        return list;
    }

    
     //Retrieves attendance records within an inclusive date range[cite: 17].
    
    public List<AttendanceLog> getLogsByDateRange(String startDate, String endDate) throws SQLException {
        List<AttendanceLog> list = new ArrayList<>();
        String sql = "SELECT log_id, employee_id, log_date, time_in, time_out, status "
                   + "FROM attendance_logs WHERE log_date BETWEEN ? AND ? ORDER BY log_date DESC, log_id DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDate(1, Date.valueOf(startDate));
            ps.setDate(2, Date.valueOf(endDate));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToAttendanceLog(rs));
                }
            }
        }
        return list;
    }

    
     //Retrieves attendance logs joined with employee and department profiles for display in a JTable.SWING.
   
    public List<Object[]> getDailyAttendanceReport(String targetDate) throws SQLException {
        List<Object[]> rows = new ArrayList<>();
        String sql = "SELECT a.log_id, a.employee_id, e.first_name, e.last_name, "
                   + "d.department_name, a.log_date, a.time_in, a.time_out, a.status "
                   + "FROM attendance_logs a "
                   + "INNER JOIN employees e ON a.employee_id = e.employee_id "
                   + "INNER JOIN departments d ON e.department_id = d.department_id "
                   + "WHERE a.log_date = ? "
                   + "ORDER BY a.log_id ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDate(1, Date.valueOf(targetDate));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Time timeInVal = rs.getTime("time_in");
                    Time timeOutVal = rs.getTime("time_out");

                    rows.add(new Object[] {
                        rs.getInt("log_id"),
                        rs.getInt("employee_id"),
                        rs.getString("first_name") + " " + rs.getString("last_name"),
                        rs.getString("department_name"),
                        rs.getDate("log_date").toString(),
                        timeInVal != null ? timeInVal.toString() : "--:--:--",
                        timeOutVal != null ? timeOutVal.toString() : "--:--:--",
                        rs.getString("status")
                    });
                }
            }
        }
        return rows;
    }

    
     // Counts the total number of distinct employees who logged attendance on a given date
    public int getPresentEmployeeCount(String logDate) throws SQLException {
        String sql = "SELECT COUNT(DISTINCT employee_id) FROM attendance_logs WHERE log_date = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDate(1, Date.valueOf(logDate));

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    
     //Maps a single JDBC ResultSet cursor row to an AttendanceLog entity instance
   
    private AttendanceLog mapResultSetToAttendanceLog(ResultSet rs) throws SQLException {
        AttendanceLog log = new AttendanceLog();
        log.setLogId(rs.getInt("log_id"));
        log.setEmployeeId(rs.getInt("employee_id"));
        log.setLogDate(rs.getDate("log_date").toString());

        Time timeIn = rs.getTime("time_in");
        log.setTimeIn(timeIn != null ? timeIn.toString() : null);

        Time timeOut = rs.getTime("time_out");
        log.setTimeOut(timeOut != null ? timeOut.toString() : null);

        log.setStatus(rs.getString("status"));
        return log;
    }
}
