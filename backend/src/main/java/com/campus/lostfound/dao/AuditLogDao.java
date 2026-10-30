package com.campus.lostfound.dao;

import com.campus.lostfound.config.DatabaseConfig;
import com.campus.lostfound.model.AuditLog;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Repository
public class AuditLogDao {

    private final DatabaseConfig databaseConfig;

    public AuditLogDao(DatabaseConfig databaseConfig) {
        this.databaseConfig = databaseConfig;
    }

    public void log(String actor, String action, String targetType, String targetId, String details) {
        String sql = "INSERT INTO audit_logs (id, actor, action, targetType, targetId, details, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)";
        // Notice column names from schema: target_type, target_id
        String correctedSql = "INSERT INTO audit_logs (id, actor, action, target_type, target_id, details, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = databaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(correctedSql)) {

            ps.setString(1, java.util.UUID.randomUUID().toString());
            ps.setString(2, actor != null ? actor : "SYSTEM");
            ps.setString(3, action);
            ps.setString(4, targetType);
            ps.setString(5, targetId);
            ps.setString(6, details);
            ps.setString(7, Instant.now().toString());

            ps.executeUpdate();
        } catch (SQLException e) {
            org.slf4j.LoggerFactory.getLogger(AuditLogDao.class).error("Failed to insert audit log", e);
        }
    }

    public List<AuditLog> findAll(int limit) throws SQLException {
        String sql = "SELECT * FROM audit_logs ORDER BY created_at DESC LIMIT ?";
        List<AuditLog> list = new ArrayList<>();
        try (Connection conn = databaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, limit > 0 ? limit : 50);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new AuditLog(
                        rs.getString("id"),
                        rs.getString("actor"),
                        rs.getString("action"),
                        rs.getString("target_type"),
                        rs.getString("target_id"),
                        rs.getString("details"),
                        parseInstant(rs.getString("created_at"))
                    ));
                }
            }
        }
        return list;
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
