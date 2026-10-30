package com.campus.lostfound.controller;

import com.campus.lostfound.config.DatabaseConfig;
import com.campus.lostfound.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final DatabaseConfig databaseConfig;

    public HealthController(DatabaseConfig databaseConfig) {
        this.databaseConfig = databaseConfig;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "Campus Lost & Found AI System");
        health.put("version", "1.0.0");

        boolean dbOk = false;
        try (Connection conn = databaseConfig.getConnection()) {
            dbOk = conn.isValid(2);
        } catch (Exception e) {
            dbOk = false;
        }

        health.put("database", dbOk ? "CONNECTED (SQLite)" : "DISCONNECTED");
        return ResponseEntity.ok(ApiResponse.ok("System healthy", health));
    }
}
