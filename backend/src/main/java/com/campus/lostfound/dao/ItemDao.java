package com.campus.lostfound.dao;

import com.campus.lostfound.config.DatabaseConfig;
import com.campus.lostfound.model.Item;
import com.campus.lostfound.model.ItemStatus;
import com.campus.lostfound.model.ReportType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class ItemDao {

    private final DatabaseConfig databaseConfig;
    private final ObjectMapper objectMapper;

    public ItemDao(DatabaseConfig databaseConfig, ObjectMapper objectMapper) {
        this.databaseConfig = databaseConfig;
        this.objectMapper = objectMapper;
    }

    public void insert(Item item) throws SQLException {
        String sql = """
            INSERT INTO items (
                id, report_type, category, description, color, brand,
                distinctive_features, search_keywords, image_path, location,
                incident_date, status, reporter_name, reporter_contact,
                reporter_user_id, private_details, created_at, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = databaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, item.getId());
            ps.setString(2, item.getReportType().name());
            ps.setString(3, item.getCategory());
            ps.setString(4, item.getDescription());
            ps.setString(5, item.getColor());
            ps.setString(6, item.getBrand());
            ps.setString(7, serializeList(item.getDistinctiveFeatures()));
            ps.setString(8, serializeList(item.getSearchKeywords()));
            ps.setString(9, item.getImagePath());
            ps.setString(10, item.getLocation());
            ps.setString(11, item.getIncidentDate());
            ps.setString(12, item.getStatus().name());
            ps.setString(13, item.getReporterName());
            ps.setString(14, item.getReporterContact());
            ps.setString(15, item.getReporterUserId());
            ps.setString(16, item.getPrivateDetails());
            ps.setString(17, item.getCreatedAt().toString());
            ps.setString(18, item.getUpdatedAt().toString());

            ps.executeUpdate();
        }
    }

    public Optional<Item> findById(String id) throws SQLException {
        String sql = "SELECT * FROM items WHERE id = ?";
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

    public List<Item> findAll(String statusFilter, String typeFilter, String search) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT * FROM items WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (statusFilter != null && !statusFilter.isBlank()) {
            sql.append(" AND status = ?");
            params.add(statusFilter);
        }
        if (typeFilter != null && !typeFilter.isBlank()) {
            sql.append(" AND report_type = ?");
            params.add(typeFilter);
        }
        if (search != null && !search.isBlank()) {
            sql.append(" AND (category LIKE ? OR description LIKE ? OR brand LIKE ? OR color LIKE ? OR location LIKE ?)");
            String term = "%" + search.trim() + "%";
            for (int i = 0; i < 5; i++) {
                params.add(term);
            }
        }
        sql.append(" ORDER BY created_at DESC");

        List<Item> items = new ArrayList<>();
        try (Connection conn = databaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(mapRow(rs));
                }
            }
        }
        return items;
    }

    public List<Item> findActiveByReportType(ReportType reportType) throws SQLException {
        String sql = "SELECT * FROM items WHERE report_type = ? AND status IN ('OPEN', 'MATCHED', 'CLAIMED') ORDER BY created_at DESC";
        List<Item> items = new ArrayList<>();
        try (Connection conn = databaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, reportType.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(mapRow(rs));
                }
            }
        }
        return items;
    }

    public boolean updateStatus(String id, ItemStatus newStatus) throws SQLException {
        String sql = "UPDATE items SET status = ?, updated_at = ? WHERE id = ?";
        try (Connection conn = databaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, newStatus.name());
            ps.setString(2, Instant.now().toString());
            ps.setString(3, id);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean deleteById(String id) throws SQLException {
        String sql = "DELETE FROM items WHERE id = ?";
        try (Connection conn = databaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    private Item mapRow(ResultSet rs) throws SQLException {
        Item item = new Item();
        item.setId(rs.getString("id"));
        item.setReportType(ReportType.valueOf(rs.getString("report_type")));
        item.setCategory(rs.getString("category"));
        item.setDescription(rs.getString("description"));
        item.setColor(rs.getString("color"));
        item.setBrand(rs.getString("brand"));
        item.setDistinctiveFeatures(deserializeList(rs.getString("distinctive_features")));
        item.setSearchKeywords(deserializeList(rs.getString("search_keywords")));
        item.setImagePath(rs.getString("image_path"));
        item.setLocation(rs.getString("location"));
        item.setIncidentDate(rs.getString("incident_date"));
        item.setStatus(ItemStatus.valueOf(rs.getString("status")));
        item.setReporterName(rs.getString("reporter_name"));
        item.setReporterContact(rs.getString("reporter_contact"));
        item.setReporterUserId(rs.getString("reporter_user_id"));
        item.setPrivateDetails(rs.getString("private_details"));
        item.setCreatedAt(parseInstant(rs.getString("created_at")));
        item.setUpdatedAt(parseInstant(rs.getString("updated_at")));
        return item;
    }

    public List<Item> findByReporterUserId(String reporterUserId) throws SQLException {
        String sql = "SELECT * FROM items WHERE reporter_user_id = ? ORDER BY created_at DESC";
        List<Item> items = new ArrayList<>();
        try (Connection conn = databaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, reporterUserId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(mapRow(rs));
                }
            }
        }
        return items;
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
