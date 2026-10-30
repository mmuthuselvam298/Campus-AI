package com.campus.lostfound.dao;

import com.campus.lostfound.config.DatabaseConfig;
import com.campus.lostfound.model.MatchRecord;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Repository
public class MatchDao {

    private final DatabaseConfig databaseConfig;
    private final ObjectMapper objectMapper;

    public MatchDao(DatabaseConfig databaseConfig, ObjectMapper objectMapper) {
        this.databaseConfig = databaseConfig;
        this.objectMapper = objectMapper;
    }

    public void upsertMatch(MatchRecord record) throws SQLException {
        String sql = """
            INSERT INTO matches (id, lost_item_id, found_item_id, match_score, match_reasons, review_status, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(lost_item_id, found_item_id) DO UPDATE SET
                match_score = excluded.match_score,
                match_reasons = excluded.match_reasons,
                review_status = excluded.review_status
            """;

        try (Connection conn = databaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, record.getId());
            ps.setString(2, record.getLostItemId());
            ps.setString(3, record.getFoundItemId());
            ps.setDouble(4, record.getMatchScore());
            ps.setString(5, serializeList(record.getMatchReasons()));
            ps.setString(6, record.getReviewStatus());
            ps.setString(7, record.getCreatedAt().toString());

            ps.executeUpdate();
        }
    }

    public List<MatchRecord> findMatchesForItem(String itemId) throws SQLException {
        String sql = "SELECT * FROM matches WHERE (lost_item_id = ? OR found_item_id = ?) AND review_status != 'DISMISSED' ORDER BY match_score DESC";
        List<MatchRecord> list = new ArrayList<>();
        try (Connection conn = databaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, itemId);
            ps.setString(2, itemId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    public boolean updateStatus(String matchId, String newStatus) throws SQLException {
        String sql = "UPDATE matches SET review_status = ? WHERE id = ?";
        try (Connection conn = databaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, newStatus);
            ps.setString(2, matchId);
            return ps.executeUpdate() > 0;
        }
    }

    private MatchRecord mapRow(ResultSet rs) throws SQLException {
        MatchRecord rec = new MatchRecord();
        rec.setId(rs.getString("id"));
        rec.setLostItemId(rs.getString("lost_item_id"));
        rec.setFoundItemId(rs.getString("found_item_id"));
        rec.setMatchScore(rs.getDouble("match_score"));
        rec.setMatchReasons(deserializeList(rs.getString("match_reasons")));
        rec.setReviewStatus(rs.getString("review_status"));
        rec.setCreatedAt(Instant.parse(rs.getString("created_at")));
        return rec;
    }

    private String serializeList(List<String> list) {
        if (list == null || list.isEmpty()) return "[]";
        try {
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            return "[]";
        }
    }

    private List<String> deserializeList(String json) {
        if (json == null || json.isBlank() || json.equals("[]")) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
