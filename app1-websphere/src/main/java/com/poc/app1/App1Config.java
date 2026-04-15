package com.poc.app1;

import com.poc.smartjdbc.SmartRoutingDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.sql.DataSource;

@Configuration
@EnableWebMvc
@ComponentScan(basePackages = "com.poc.app1")
public class App1Config implements WebMvcConfigurer {

    @Bean
    public DataSource dataSource() {
        DriverManagerDataSource oracle = new DriverManagerDataSource();
        oracle.setDriverClassName("oracle.jdbc.OracleDriver");
        oracle.setUrl("jdbc:oracle:thin:@oracle-primary:1521:FREE");
        oracle.setUsername("system");
        oracle.setPassword("oracle");

        DriverManagerDataSource postgres = new DriverManagerDataSource();
        postgres.setDriverClassName("org.postgresql.Driver");
        postgres.setUrl("jdbc:postgresql://postgres:5432/mydb");
        postgres.setUsername("postgres");
        postgres.setPassword("postgres");

        // using consul-server1 since it'll run in docker compose
        SmartRoutingDataSource router = new SmartRoutingDataSource(oracle, postgres, "http://consul-server1:8500");
        router.afterPropertiesSet();
        return router;
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
