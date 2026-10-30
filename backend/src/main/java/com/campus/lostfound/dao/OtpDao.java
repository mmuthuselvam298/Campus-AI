package com.campus.lostfound.dao;

import com.campus.lostfound.config.DatabaseConfig;
import com.campus.lostfound.model.OtpToken;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;

@Repository
public class OtpDao {

    private final DatabaseConfig databaseConfig;

    public OtpDao(DatabaseConfig databaseConfig) {
        this.databaseConfig = databaseConfig;
    }

    public void insert(OtpToken token) throws SQLException {
        String sql = """
            INSERT INTO otp_tokens (
                id, email, otp_hash, salt, attempts, max_attempts,
                expires_at, consumed, created_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = databaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, token.getId());
            ps.setString(2, token.getEmail().toLowerCase().trim());
            ps.setString(3, token.getOtpHash());
            ps.setString(4, token.getSalt());
            ps.setInt(5, token.getAttempts());
            ps.setInt(6, token.getMaxAttempts());
            ps.setString(7, token.getExpiresAt().toString());
            ps.setInt(8, token.isConsumed() ? 1 : 0);
            ps.setString(9, token.getCreatedAt().toString());

            ps.executeUpdate();
        }
    }

    public Optional<OtpToken> findLatestActiveByEmail(String email) throws SQLException {
        String sql = """
            SELECT * FROM otp_tokens
            WHERE LOWER(email) = LOWER(?) AND consumed = 0
            ORDER BY created_at DESC LIMIT 1
            """;

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

    public void incrementAttempts(String id) throws SQLException {
        String sql = "UPDATE otp_tokens SET attempts = attempts + 1 WHERE id = ?";
        try (Connection conn = databaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, id);
            ps.executeUpdate();
        }
    }

    public void markConsumed(String id) throws SQLException {
        String sql = "UPDATE otp_tokens SET consumed = 1 WHERE id = ?";
        try (Connection conn = databaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, id);
            ps.executeUpdate();
        }
    }

    public int countRecentRequests(String email, Instant since) throws SQLException {
        String sql = "SELECT COUNT(*) FROM otp_tokens WHERE LOWER(email) = LOWER(?) AND created_at >= ?";
        try (Connection conn = databaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, email.trim());
            ps.setString(2, since.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    private OtpToken mapRow(ResultSet rs) throws SQLException {
        OtpToken t = new OtpToken();
        t.setId(rs.getString("id"));
        t.setEmail(rs.getString("email"));
        t.setOtpHash(rs.getString("otp_hash"));
        t.setSalt(rs.getString("salt"));
        t.setAttempts(rs.getInt("attempts"));
        t.setMaxAttempts(rs.getInt("max_attempts"));
        t.setExpiresAt(parseInstant(rs.getString("expires_at")));
        t.setConsumed(rs.getInt("consumed") == 1);
        t.setCreatedAt(parseInstant(rs.getString("created_at")));
        return t;
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
