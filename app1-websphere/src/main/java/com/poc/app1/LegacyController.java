package com.poc.app1;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LegacyController {
    
    private final JdbcTemplate jdbcTemplate;
    
    public LegacyController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/legacy-query")
    public String legacyQuery() {
        // Example legacy Oracle query that gets dynamically rewritten by SmartRoutingDataSource if routed to Postgres
        return jdbcTemplate.queryForObject("SELECT NVL('100', '0') FROM DUAL", String.class);
    }

    @GetMapping("/users")
    public String getUsers() {
        return jdbcTemplate.queryForList("SELECT id, name FROM app_user").toString();
    }
}
