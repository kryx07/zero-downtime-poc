package com.poc.cdc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.jdbc.DataSourceBuilder;

import javax.sql.DataSource;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@SpringBootApplication
@EnableScheduling
public class CdcWorkerApplication {
    public static void main(String[] args) {
        System.setProperty("server.port", "8083");
        SpringApplication.run(CdcWorkerApplication.class, args);
    }
    
    @Bean(name = "oracleDs")
    public DataSource oracleDataSource() {
        return DataSourceBuilder.create().url("jdbc:oracle:thin:@localhost:1521:FREE").username("system").password("oracle").build();
    }
    
    @Bean(name = "postgresDs")
    public DataSource postgresDataSource() {
        return DataSourceBuilder.create().url("jdbc:postgresql://localhost:5432/mydb").username("postgres").password("postgres").build();
    }
}

@Component
class ReplicationWorker {
    private static final Logger log = Logger.getLogger(ReplicationWorker.class.getName());
    private final JdbcTemplate oracle;
    private final JdbcTemplate postgres;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public ReplicationWorker(@Qualifier("oracleDs") DataSource oracleDs, @Qualifier("postgresDs") DataSource pgDs) {
        this.oracle = new JdbcTemplate(oracleDs);
        this.postgres = new JdbcTemplate(pgDs);
    }

    @Scheduled(fixedDelay = 1000)
    public void replicate() throws Exception {
        String activeDb = getConsulState();
        
        if ("ORACLE".equals(activeDb) || "DRAINING_TO_POSTGRES".equals(activeDb)) {
            try {
                List<Map<String, Object>> outbox = oracle.queryForList("SELECT id, payload FROM outbox WHERE processed = 0");
                for (Map<String, Object> row : outbox) {
                    postgres.update("INSERT INTO target_table (payload) VALUES (?)", row.get("PAYLOAD"));
                    oracle.update("UPDATE outbox SET processed = 1 WHERE id = ?", row.get("ID"));
                }
                
                if ("DRAINING_TO_POSTGRES".equals(activeDb) && outbox.isEmpty()) {
                    log.info("Drain complete! Promoting Postgres...");
                    setConsulState("POSTGRES");
                }
            } catch (Exception e) {
                log.warning("Oracle -> Postgres replication error: " + e.getMessage());
            }
        } else if ("POSTGRES".equals(activeDb)) {
            try {
                List<Map<String, Object>> outbox = postgres.queryForList("SELECT id, payload FROM outbox WHERE processed = false");
                for (Map<String, Object> row : outbox) {
                    oracle.update("INSERT INTO target_table (payload) VALUES (?)", row.get("payload"));
                    postgres.update("UPDATE outbox SET processed = true WHERE id = ?", row.get("id"));
                }
            } catch (Exception e) {
                log.warning("Postgres -> Oracle replication error: " + e.getMessage());
            }
        }
    }

    private String getConsulState() throws Exception {
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create("http://localhost:8500/v1/kv/db/active")).GET().build();
        HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() == 200) {
            String body = res.body();
            if (body.contains("\"Value\":\"")) {
                int start = body.indexOf("\"Value\":\"") + 9;
                int end = body.indexOf("\"", start);
                String b64 = body.substring(start, end);
                return new String(Base64.getDecoder().decode(b64));
            }
        }
        return "ORACLE";
    }
    
    private void setConsulState(String state) throws Exception {
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create("http://localhost:8500/v1/kv/db/active"))
            .PUT(HttpRequest.BodyPublishers.ofString(state)).build();
        httpClient.send(req, HttpResponse.BodyHandlers.ofString());
    }
}
