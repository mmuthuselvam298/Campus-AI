package com.campus.lostfound.dao;

import com.campus.lostfound.config.DatabaseConfig;
import com.campus.lostfound.model.AccountStatus;
import com.campus.lostfound.model.User;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class UserDao {

    private final DatabaseConfig databaseConfig;

    public UserDao(DatabaseConfig databaseConfig) {
        this.databaseConfig = databaseConfig;
    }

    public void insert(User user) throws SQLException {
        String sql = """
            INSERT INTO users (
                id, full_name, campus_email, role, account_status,
                requested_role, email_verified, approved_by, approved_at,
                created_at, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = databaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, user.getId());
            ps.setString(2, user.getFullName());
            ps.setString(3, user.getCampusEmail().toLowerCase().trim());
            ps.setString(4, user.getRole());
            ps.setString(5, user.getAccountStatus() != null ? user.getAccountStatus().name() : AccountStatus.ACTIVE.name());
            ps.setString(6, user.getRequestedRole());
            ps.setInt(7, user.isEmailVerified() ? 1 : 0);
            ps.setString(8, user.getApprovedBy());
            ps.setString(9, user.getApprovedAt() != null ? user.getApprovedAt().toString() : null);
            ps.setString(10, user.getCreatedAt().toString());
            ps.setString(11, user.getUpdatedAt().toString());

            ps.executeUpdate();
        }
    }

    public void update(User user) throws SQLException {
        String sql = """
            UPDATE users SET
                full_name = ?,
                role = ?,
                account_status = ?,
                requested_role = ?,
                email_verified = ?,
                approved_by = ?,
                approved_at = ?,
                updated_at = ?
            WHERE id = ?
            """;

        try (Connection conn = databaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, user.getFullName());
            ps.setString(2, user.getRole());
            ps.setString(3, user.getAccountStatus().name());
            ps.setString(4, user.getRequestedRole());
            ps.setInt(5, user.isEmailVerified() ? 1 : 0);
            ps.setString(6, user.getApprovedBy());
            ps.setString(7, user.getApprovedAt() != null ? user.getApprovedAt().toString() : null);
            ps.setString(8, Instant.now().toString());
            ps.setString(9, user.getId());

            ps.executeUpdate();
        }
    }

    public boolean updateStaffStatus(String userId, AccountStatus status, String approvedBy) throws SQLException {
        String sql = """
            UPDATE users SET
                account_status = ?,
                approved_by = ?,
                approved_at = ?,
                role = CASE WHEN ? = 'ACTIVE' THEN 'STAFF' ELSE role END,
                updated_at = ?
            WHERE id = ?
            """;

        try (Connection conn = databaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, status.name());
            ps.setString(2, approvedBy);
            ps.setString(3, Instant.now().toString());
            ps.setString(4, status.name());
            ps.setString(5, Instant.now().toString());
            ps.setString(6, userId);

            return ps.executeUpdate() > 0;
        }
    }

    public Optional<User> findByEmail(String email) throws SQLException {
        if (email == null) return Optional.empty();
        String sql = "SELECT * FROM users WHERE LOWER(campus_email) = LOWER(?)";
        try (Connection conn = databaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, email.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    public Optional<User> findById(String id) throws SQLException {
        if (id == null) return Optional.empty();
        String sql = "SELECT * FROM users WHERE id = ?";
        try (Connection conn = databaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    public List<User> findStaffRequests() throws SQLException {
        String sql = "SELECT * FROM users WHERE requested_role = 'STAFF' ORDER BY created_at DESC";
        List<User> list = new ArrayList<>();
        try (Connection conn = databaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    private User mapRow(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getString("id"));
        u.setFullName(rs.getString("full_name"));
        u.setCampusEmail(rs.getString("campus_email"));
        u.setRole(rs.getString("role"));

        String statusStr = rs.getString("account_status");
        u.setAccountStatus(statusStr != null ? AccountStatus.valueOf(statusStr) : AccountStatus.ACTIVE);

        u.setRequestedRole(rs.getString("requested_role"));
        u.setEmailVerified(rs.getInt("email_verified") == 1);
        u.setApprovedBy(rs.getString("approved_by"));

        String approvedAtStr = rs.getString("approved_at");
        if (approvedAtStr != null && !approvedAtStr.isBlank()) {
            u.setApprovedAt(parseInstant(approvedAtStr));
        }

        u.setCreatedAt(parseInstant(rs.getString("created_at")));

        String updatedStr = rs.getString("updated_at");
        if (updatedStr != null && !updatedStr.isBlank()) {
            u.setUpdatedAt(parseInstant(updatedStr));
        } else {
            u.setUpdatedAt(u.getCreatedAt());
        }

        return u;
    }

    private Instant parseInstant(String str) {
        if (str == null || str.isBlank()) {
            return Instant.now();
        }
        try {
            return Instant.parse(str.trim());
        } catch (Exception e) {
            try {
                String normalized = str.trim().replace(" ", "T");
                if (!normalized.endsWith("Z") && !normalized.contains("+")) {
                    normalized += "Z";
                }
                return Instant.parse(normalized);
            } catch (Exception ex) {
                return Instant.now();
            }
        }
    }
}
