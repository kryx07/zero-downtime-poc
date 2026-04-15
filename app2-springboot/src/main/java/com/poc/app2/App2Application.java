package com.poc.app2;

import com.poc.smartjdbc.SmartRoutingDataSource;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.repository.CrudRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.boot.jdbc.DataSourceBuilder;
import jakarta.persistence.Table;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@SpringBootApplication
public class App2Application {
    public static void main(String[] args) {
        System.setProperty("server.port", "8082");
        SpringApplication.run(App2Application.class, args);
    }

    @Bean
    @Primary
    public DataSource dataSource() {
        DataSource oracle = DataSourceBuilder.create().url("jdbc:oracle:thin:@oracle-primary:1521:FREE").username("system").password("oracle").build();
        DataSource postgres = DataSourceBuilder.create().url("jdbc:postgresql://postgres:5432/mydb").username("postgres").password("postgres").build();
        
        SmartRoutingDataSource router = new SmartRoutingDataSource(oracle, postgres, "http://consul-server1:8500");
        router.afterPropertiesSet();
        return router;
    }
}

@Entity
@Table(name = "app_user")
class AppUser {
    @Id
    public String id;
    public String name;
}

interface UserRepository extends CrudRepository<AppUser, String> {}

@RestController
class UserController {
    private final UserRepository repo;
    public UserController(UserRepository repo) {
        this.repo = repo;
    }

    @GetMapping("/users")
    public Iterable<AppUser> getUsers() {
        return repo.findAll();
    }
}
