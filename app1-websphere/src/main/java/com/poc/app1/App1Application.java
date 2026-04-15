package com.poc.app1;

import com.poc.smartjdbc.SmartRoutingDataSource;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.boot.jdbc.DataSourceBuilder;

import javax.sql.DataSource;

@SpringBootApplication
public class App1Application {
    public static void main(String[] args) {
        System.setProperty("server.port", "8081");
        SpringApplication.run(App1Application.class, args);
    }

    @Bean
    public DataSource dataSource() {
        DataSource oracle = DataSourceBuilder.create().url("jdbc:oracle:thin:@localhost:1521:FREE").username("system").password("oracle").build();
        DataSource postgres = DataSourceBuilder.create().url("jdbc:postgresql://localhost:5432/mydb").username("postgres").password("postgres").build();
        
        SmartRoutingDataSource router = new SmartRoutingDataSource(oracle, postgres, "http://localhost:8500");
        router.afterPropertiesSet();
        return router;
    }
}

@RestController
class LegacyController {
    private final JdbcTemplate jdbcTemplate;
    public LegacyController(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @GetMapping("/legacy-query")
    public String legacyQuery() {
        // Example legacy Oracle query that gets dynamically rewritten by SmartRoutingDataSource if routed to Postgres
        return jdbcTemplate.queryForObject("SELECT NVL('100', '0') FROM DUAL", String.class);
    }
}
