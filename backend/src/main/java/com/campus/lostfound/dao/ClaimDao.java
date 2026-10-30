package com.campus.lostfound.dao;

import com.campus.lostfound.config.DatabaseConfig;
import com.campus.lostfound.model.Claim;
import com.campus.lostfound.model.ClaimStatus;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class ClaimDao {

    private final DatabaseConfig databaseConfig;

    public ClaimDao(DatabaseConfig databaseConfig) {
        this.databaseConfig = databaseConfig;
    }

    public void insert(Claim claim) throws SQLException {
        String sql = """
            INSERT INTO claims (
                id, item_id, claimant_user_id, claimant_name, claimant_email, claim_message,
                proof_details, status, resolution_notes, resolved_by,
                created_at, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = databaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, claim.getId());
            ps.setString(2, claim.getItemId());
            ps.setString(3, claim.getClaimantUserId());
            ps.setString(4, claim.getClaimantName());
            ps.setString(5, claim.getClaimantEmail());
            ps.setString(6, claim.getClaimMessage());
            ps.setString(7, claim.getProofDetails());
            ps.setString(8, claim.getStatus().name());
            ps.setString(9, claim.getResolutionNotes());
            ps.setString(10, claim.getResolvedBy());
            ps.setString(11, claim.getCreatedAt().toString());
            ps.setString(12, claim.getUpdatedAt().toString());

            ps.executeUpdate();
        }
    }

    public Optional<Claim> findById(String id) throws SQLException {
        String sql = "SELECT * FROM claims WHERE id = ?";
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

    public List<Claim> findByItemId(String itemId) throws SQLException {
        String sql = "SELECT * FROM claims WHERE item_id = ? ORDER BY created_at DESC";
        List<Claim> claims = new ArrayList<>();
        try (Connection conn = databaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, itemId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    claims.add(mapRow(rs));
                }
            }
        }
        return claims;
    }

    public List<Claim> findAll(String statusFilter) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT * FROM claims");
        if (statusFilter != null && !statusFilter.isBlank()) {
            sql.append(" WHERE status = ?");
        }
        sql.append(" ORDER BY created_at DESC");

        List<Claim> claims = new ArrayList<>();
        try (Connection conn = databaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            if (statusFilter != null && !statusFilter.isBlank()) {
                ps.setString(1, statusFilter);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    claims.add(mapRow(rs));
                }
            }
        }
        return claims;
    }

    public boolean updateResolution(String id, ClaimStatus status, String resolutionNotes, String resolvedBy) throws SQLException {
        String sql = "UPDATE claims SET status = ?, resolution_notes = ?, resolved_by = ?, updated_at = ? WHERE id = ?";
        try (Connection conn = databaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, status.name());
            ps.setString(2, resolutionNotes);
            ps.setString(3, resolvedBy);
            ps.setString(4, Instant.now().toString());
            ps.setString(5, id);
            return ps.executeUpdate() > 0;
        }
    }

    public List<Claim> findByClaimantUserId(String claimantUserId) throws SQLException {
        String sql = "SELECT * FROM claims WHERE claimant_user_id = ? ORDER BY created_at DESC";
        List<Claim> claims = new ArrayList<>();
        try (Connection conn = databaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, claimantUserId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    claims.add(mapRow(rs));
                }
            }
        }
        return claims;
    }

    private Claim mapRow(ResultSet rs) throws SQLException {
        Claim claim = new Claim();
        claim.setId(rs.getString("id"));
        claim.setItemId(rs.getString("item_id"));
        claim.setClaimantUserId(rs.getString("claimant_user_id"));
        claim.setClaimantName(rs.getString("claimant_name"));
        claim.setClaimantEmail(rs.getString("claimant_email"));
        claim.setClaimMessage(rs.getString("claim_message"));
        claim.setProofDetails(rs.getString("proof_details"));
        claim.setStatus(ClaimStatus.valueOf(rs.getString("status")));
        claim.setResolutionNotes(rs.getString("resolution_notes"));
        claim.setResolvedBy(rs.getString("resolved_by"));
        claim.setCreatedAt(parseInstant(rs.getString("created_at")));
        claim.setUpdatedAt(parseInstant(rs.getString("updated_at")));
        return claim;
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
