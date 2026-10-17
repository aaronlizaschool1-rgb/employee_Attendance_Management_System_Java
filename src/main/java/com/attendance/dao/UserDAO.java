/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.attendance.dao;

import com.attendance.model.User;
import com.attendance.util.DatabaseConnection;
import com.attendance.util.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Aaron
 *
 * Password hashing added: passwords are stored as salted PBKDF2 hashes (see PasswordUtil),
 * never as plain text. Callers keep passing plain-text passwords; this class hashes them.
 */
public class UserDAO {

    /**
     * Authenticates credentials against the users table.
     * The row is found by username (ACTIVE accounts only), then the typed password
     * is checked against the stored hash.
     * Returns the User on success, or null for a wrong username/password or inactive account.
     */
    public User authenticate(String username, String password) throws SQLException {
        String sql = "SELECT user_id, username, password, full_name, role, status "
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
     * Inserts a new system user record - ROLE MUST SATISFY User.role - use input validation in business logic side/ui.
     * The password is hashed before it is saved.
     */
    public boolean addUser(User user) throws SQLException {
        String sql = "INSERT INTO users (username, password, full_name, role, status) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, user.getUsername());
            ps.setString(2, hashIfNeeded(user.getPassword()));
            ps.setString(3, user.getFullName());
            ps.setString(4, user.getRole());
            ps.setString(5, user.getStatus() != null ? user.getStatus() : "ACTIVE");

            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Retrieves all system users ordered by user_id
     */
    public List<User> getAllUsers() throws SQLException {
        List<User> list = new ArrayList<>();
        String sql = "SELECT user_id, username, password, full_name, role, status FROM users ORDER BY user_id ASC";

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
     * Finds a user by their primary key ID - input validation also in users.user_id
     */
    public User getUserById(int userId) throws SQLException {
        String sql = "SELECT user_id, username, password, full_name, role, status FROM users WHERE user_id = ?";

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
     * Checks if a username is already taken to avoid unique constraint violations - input validation for users.username
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
     * Updates an existing user's details and credentials - input validation.
     * If the User object still holds an already-hashed password (for example it came from
     * getUserById), it is saved as is. A plain-text password is hashed first.
     */
    public boolean updateUser(User user) throws SQLException {
        String sql = "UPDATE users SET username = ?, password = ?, full_name = ?, role = ?, status = ? WHERE user_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, user.getUsername());
            ps.setString(2, hashIfNeeded(user.getPassword()));
            ps.setString(3, user.getFullName());
            ps.setString(4, user.getRole());
            ps.setString(5, user.getStatus());
            ps.setInt(6, user.getUserId());

            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Updates only a user's password. Pass the NEW PLAIN-TEXT password; it is always hashed here.
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
     * Deletes a user by users.user_id - check if its deleted ON DELETE NULL - input validation
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
     * One-time conversion: hashes every password that is still stored as plain text
     * (for example the seed accounts). Passwords that are already hashed are left alone,
     * so this is safe to call on every start. Returns how many accounts were converted.
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

    private String hashIfNeeded(String password) {
        return PasswordUtil.isHashed(password) ? password : PasswordUtil.hash(password);
    }

    /**
     * Maps a single ResultSet cursor row to a User model object
     */
    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setUserId(rs.getInt("user_id"));
        user.setUsername(rs.getString("username"));
        user.setPassword(rs.getString("password"));
        user.setFullName(rs.getString("full_name"));
        user.setRole(rs.getString("role"));
        user.setStatus(rs.getString("status"));
        return user;
    }
}
