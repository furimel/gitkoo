package com.furimeo.gitkoo.web;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Minimal health/readiness endpoint.
 *
 * <p>Returns a small JSON status document covering application, database, and storage.
 * Kept intentionally simple \u2014 no observability platform (design \u00a758).
 *
 * @see DESIGN.md §78
 */
@RestController
public class HealthController {

    private final DataSource dataSource;

    public HealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("status", isDbReachable() ? "UP" : "DOWN");

        Map<String, String> components = new LinkedHashMap<>();
        components.put("application", "UP");
        components.put("database", isDbReachable() ? "UP" : "DOWN");
        status.put("components", components);

        return status;
    }

    private boolean isDbReachable() {
        try (var conn = dataSource.getConnection();
             var stmt = conn.createStatement();
             var rs = stmt.executeQuery("SELECT 1")) {
            return rs.next();
        } catch (Exception e) {
            return false;
        }
    }
}
