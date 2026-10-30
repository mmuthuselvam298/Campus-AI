package com.campus.lostfound.dao;

import com.campus.lostfound.config.DatabaseConfig;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class DatabaseInitializer {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseInitializer.class);
    private final DatabaseConfig databaseConfig;

    public DatabaseInitializer(DatabaseConfig databaseConfig) {
        this.databaseConfig = databaseConfig;
    }

    @PostConstruct
    public void initialize() {
        logger.info("Initializing SQLite database at: {}", databaseConfig.getDatabasePath());
        try (Connection conn = databaseConfig.getConnection();
             Statement stmt = conn.createStatement()) {

            // 1. If database already has pre-existing tables from prior runs, upgrade columns first so subsequent index creation succeeds
            safeMigrate(conn);

            // 2. Run base schema with CREATE TABLE / INDEX IF NOT EXISTS
            ClassPathResource resource = new ClassPathResource("schema.sql");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                String sql = reader.lines().collect(Collectors.joining("\n"));
                String[] statements = sql.split(";");
                for (String singleStatement : statements) {
                    String trimmed = singleStatement.trim();
                    if (!trimmed.isEmpty()) {
                        stmt.execute(trimmed);
                    }
                }
            }

            // 3. Run safeMigrate again to ensure all columns exist
            safeMigrate(conn);

            logger.info("SQLite schema initialized and verified. Database is clean and ready.");
        } catch (Exception e) {
            logger.error("Failed to initialize SQLite database", e);
            throw new RuntimeException("Could not initialize database", e);
        }
    }

    private void safeMigrate(Connection conn) throws SQLException {
        if (!tableExists(conn, "users")) {
            return;
        }

        try (Statement stmt = conn.createStatement()) {
            // users table columns migration
            Set<String> userCols = getExistingColumns(conn, "users");
            if (!userCols.contains("account_status")) {
                stmt.execute("ALTER TABLE users ADD COLUMN account_status TEXT NOT NULL DEFAULT 'ACTIVE'");
            }
            if (!userCols.contains("requested_role")) {
                stmt.execute("ALTER TABLE users ADD COLUMN requested_role TEXT");
            }
            if (!userCols.contains("email_verified")) {
                stmt.execute("ALTER TABLE users ADD COLUMN email_verified INTEGER NOT NULL DEFAULT 0");
            }
            if (!userCols.contains("approved_by")) {
                stmt.execute("ALTER TABLE users ADD COLUMN approved_by TEXT");
            }
            if (!userCols.contains("approved_at")) {
                stmt.execute("ALTER TABLE users ADD COLUMN approved_at TEXT");
            }
            if (!userCols.contains("updated_at")) {
                stmt.execute("ALTER TABLE users ADD COLUMN updated_at TEXT");
            }

            // items table columns migration
            if (tableExists(conn, "items")) {
                Set<String> itemCols = getExistingColumns(conn, "items");
                if (!itemCols.contains("reporter_user_id")) {
                    stmt.execute("ALTER TABLE items ADD COLUMN reporter_user_id TEXT");
                }
            }

            // claims table columns migration
            if (tableExists(conn, "claims")) {
                Set<String> claimCols = getExistingColumns(conn, "claims");
                if (!claimCols.contains("claimant_user_id")) {
                    stmt.execute("ALTER TABLE claims ADD COLUMN claimant_user_id TEXT");
                }
            }
        }
    }

    private boolean tableExists(Connection conn, String tableName) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='" + tableName + "'")) {
            return rs.next();
        }
    }

    private Set<String> getExistingColumns(Connection conn, String tableName) throws SQLException {
        Set<String> columns = new HashSet<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA table_info(" + tableName + ")")) {
            while (rs.next()) {
                columns.add(rs.getString("name").toLowerCase());
            }
        }
        return columns;
    }
}
