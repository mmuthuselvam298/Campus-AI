package com.campus.lostfound.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

@Configuration
public class DatabaseConfig {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);

    @Value("${app.database.path:./data/campus_lost_found.db}")
    private String databasePath;

    public Connection getConnection() throws SQLException {
        ensureDatabaseDirectory();
        String url = "jdbc:sqlite:" + databasePath;
        Connection conn = DriverManager.getConnection(url);
        // Enable foreign key constraints and WAL journal mode for performance & integrity
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
            stmt.execute("PRAGMA journal_mode = WAL;");
        }
        return conn;
    }

    private void ensureDatabaseDirectory() {
        File dbFile = new File(databasePath);
        File parentDir = dbFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            boolean created = parentDir.mkdirs();
            if (created) {
                logger.info("Created database directory: {}", parentDir.getAbsolutePath());
            }
        }
    }

    public String getDatabasePath() {
        return databasePath;
    }
}
