package com.attendance.dao;

import com.attendance.model.User;
import com.attendance.util.DatabaseConnection;
import com.attendance.util.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object (DAO) for the USERS table.
 * Manages user accounts, authentication via salted PBKDF2 hashing,
 * password migration, and 1:1 employee foreign key persistence.
 *
 * @author Aaron & Team
 */
public class UserDAO {

    /**
     * Authenticates credentials against the users table.
     * The row is retrieved by username (ACTIVE accounts only), then the entered
     * password is verified against the stored hash via PasswordUtil.
     *
     * @param username the entered username
     * @param password the plain-text password to verify
     * @return User object on success, or null for incorrect credentials / inactive state
     * @throws SQLException if a database access error occurs
     */
    public User authenticate(String username, String password) throws SQLException {
        String sql = "SELECT user_id, employee_id, username, password, full_name, role, status "
                   + "FROM users WHERE username = ? AND status = 'ACTIVE'";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    User user = mapResultSetToUser(rs);
                    if (PasswordUtil.verify(password, user.getPassword())) {
                        return user;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Inserts a new system user record.
     * The password is automatically hashed before persisting.
     * Supports nullable employee_id.
     *
     * @param user the User entity to add
     * @return true if added successfully, false otherwise
     * @throws SQLException if a database access error occurs
     */
    public boolean addUser(User user) throws SQLException {
        String sql = "INSERT INTO users (employee_id, username, password, full_name, role, status) "
                   + "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            if (user.getEmployeeId() != null && user.getEmployeeId() > 0) {
                ps.setInt(1, user.getEmployeeId());
            } else {
                ps.setNull(1, Types.INTEGER);
            }

            ps.setString(2, user.getUsername());
            ps.setString(3, hashIfNeeded(user.getPassword()));
            ps.setString(4, user.getFullName());
            ps.setString(5, user.getRole());
            ps.setString(6, user.getStatus() != null ? user.getStatus() : "ACTIVE");

            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Retrieves all system users ordered by user_id ascending.
     *
     * @return a list of User entities
     * @throws SQLException if a database access error occurs
     */
    public List<User> getAllUsers() throws SQLException {
        List<User> list = new ArrayList<>();
        String sql = "SELECT user_id, employee_id, username, password, full_name, role, status "
                   + "FROM users ORDER BY user_id ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapResultSetToUser(rs));
            }
        }
        return list;
    }

    /**
     * Finds a user by their primary key user_id.
     *
     * @param userId the user ID to search for
     * @return User object if found, null otherwise
     * @throws SQLException if a database access error occurs
     */
    public User getUserById(int userId) throws SQLException {
        String sql = "SELECT user_id, employee_id, username, password, full_name, role, status "
                   + "FROM users WHERE user_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }
        }
        return null;
    }

    /**
     * Finds a user account linked to a specific employee ID.
     *
     * @param employeeId the employee ID to query
     * @return User object if linked, null otherwise
     * @throws SQLException if a database access error occurs
     */
    public User getUserByEmployeeId(int employeeId) throws SQLException {
        String sql = "SELECT user_id, employee_id, username, password, full_name, role, status "
                   + "FROM users WHERE employee_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, employeeId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }
        }
        return null;
    }

    /**
     * Checks if a username is already taken to prevent unique constraint violations.
     *
     * @param username the candidate username
     * @return true if the username exists, false otherwise
     * @throws SQLException if a database access error occurs
     */
    public boolean usernameExists(String username) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    /**
     * Updates an existing user's details and credentials.
     * If the password is already hashed, it is retained; otherwise, it is hashed first.
     *
     * @param user the User entity with updated fields
     * @return true if updated successfully, false otherwise
     * @throws SQLException if a database access error occurs
     */
    public boolean updateUser(User user) throws SQLException {
        String sql = "UPDATE users SET employee_id = ?, username = ?, password = ?, "
                   + "full_name = ?, role = ?, status = ? WHERE user_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            if (user.getEmployeeId() != null && user.getEmployeeId() > 0) {
                ps.setInt(1, user.getEmployeeId());
            } else {
                ps.setNull(1, Types.INTEGER);
            }

            ps.setString(2, user.getUsername());
            ps.setString(3, hashIfNeeded(user.getPassword()));
            ps.setString(4, user.getFullName());
            ps.setString(5, user.getRole());
            ps.setString(6, user.getStatus());
            ps.setInt(7, user.getUserId());

            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Updates only a user's password credential.
     * Expects a plain-text password and hashes it before updating.
     *
     * @param userId the ID of the user
     * @param newPassword the new plain-text password
     * @return true if updated successfully, false otherwise
     * @throws SQLException if a database access error occurs
     */
    public boolean updatePassword(int userId, String newPassword) throws SQLException {
        String sql = "UPDATE users SET password = ? WHERE user_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, PasswordUtil.hash(newPassword));
            ps.setInt(2, userId);

            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Deletes a user by user_id.
     *
     * @param userId the user ID to delete
     * @return true if deleted successfully, false otherwise
     * @throws SQLException if a database access error occurs
     */
    public boolean deleteUser(int userId) throws SQLException {
        String sql = "DELETE FROM users WHERE user_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * One-time conversion: hashes any passwords still stored in plain text
     * (e.g., initial seed data). Safely callable during application bootstrap.
     *
     * @return total count of accounts migrated
     * @throws SQLException if a database access error occurs
     */
    public int migratePlaintextPasswords() throws SQLException {
        int migrated = 0;
        for (User u : getAllUsers()) {
            if (!PasswordUtil.isHashed(u.getPassword())) {
                if (updatePassword(u.getUserId(), u.getPassword())) {
                    migrated++;
                }
            }
        }
        return migrated;
    }

    /**
     * Hashes the password if it is not already in hashed format.
     */
    private String hashIfNeeded(String password) {
        return PasswordUtil.isHashed(password) ? password : PasswordUtil.hash(password);
    }

    /**
     * Maps a single ResultSet cursor row to a User model object,
     * safely converting SQL NULL employee_id into a Java null.
     */
    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setUserId(rs.getInt("user_id"));

        int empId = rs.getInt("employee_id");
        if (rs.wasNull()) {
            user.setEmployeeId(null);
        } else {
            user.setEmployeeId(empId);
        }

        user.setUsername(rs.getString("username"));
        user.setPassword(rs.getString("password"));
        user.setFullName(rs.getString("full_name"));
        user.setRole(rs.getString("role"));
        user.setStatus(rs.getString("status"));
        return user;
    }
}