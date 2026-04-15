package com.poc.smartjdbc;

import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import java.lang.reflect.Proxy;

public class SmartRoutingDataSource extends AbstractRoutingDataSource {
    private static final Logger log = Logger.getLogger(SmartRoutingDataSource.class.getName());

    public enum DbState { ORACLE, DRAINING_TO_POSTGRES, POSTGRES }
    private final AtomicReference<DbState> currentState = new AtomicReference<>(DbState.ORACLE);
    
    private final Object pauseLock = new Object();
    
    public SmartRoutingDataSource(DataSource oracle, DataSource postgres, String consulUrl) {
        Map<Object, Object> targetDataSources = Map.of(
            DbState.ORACLE, oracle,
            DbState.POSTGRES, postgres
        );
        this.setTargetDataSources(targetDataSources);
        this.setDefaultTargetDataSource(oracle);
        
        startConsulLongPolling(consulUrl);
    }

    @Override
    protected Object determineCurrentLookupKey() {
        DbState state = currentState.get();
        if (state == DbState.DRAINING_TO_POSTGRES) {
            log.info("State is DRAINING_TO_POSTGRES, parking thread...");
            synchronized (pauseLock) {
                while (currentState.get() == DbState.DRAINING_TO_POSTGRES) {
                    try { pauseLock.wait(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                }
            }
            state = currentState.get();
            log.info("Unparked! New state is " + state);
        }
        return state;
    }

    @Override
    public Connection getConnection() throws SQLException {
        Connection conn = super.getConnection();
        if (currentState.get() == DbState.POSTGRES) {
            return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class[]{Connection.class},
                (proxy, method, args) -> {
                    if (("prepareStatement".equals(method.getName()) || "createStatement".equals(method.getName())) && args != null && args.length > 0 && args[0] instanceof String sql) {
                        args[0] = translateOracleToPostgres(sql);
                    }
                    return method.invoke(conn, args);
                }
            );
        }
        return conn;
    }

    private String translateOracleToPostgres(String sql) {
        try {
            if (sql.toUpperCase().contains("DUAL") || sql.toUpperCase().contains("NVL")) {
                Statement stmt = CCJSqlParserUtil.parse(sql);
                String translated = stmt.toString()
                    .replaceAll("(?i)\\bNVL\\b", "COALESCE")
                    .replaceAll("(?i)\\bFROM DUAL\\b", "");
                log.info("Translated SQL: " + translated);
                return translated;
            }
        } catch (Exception e) {
            log.warning("JSqlParser failed: " + e.getMessage());
        }
        return sql.replaceAll("(?i)\\bNVL\\b", "COALESCE").replaceAll("(?i)\\bFROM DUAL\\b", "");
    }

    private void startConsulLongPolling(String consulUrl) {
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        Executors.newSingleThreadExecutor().submit(() -> {
            String index = "0";
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(consulUrl + "/v1/kv/db/active?index=" + index + "&wait=10s"))
                        .GET().build();
                    HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
                    
                    var headers = res.headers().map();
                    if (headers.containsKey("x-consul-index")) {
                        index = headers.get("x-consul-index").get(0);
                    }
                    
                    if (res.statusCode() == 200) {
                        String body = res.body();
                        if (body.contains("\"Value\":\"")) {
                            int start = body.indexOf("\"Value\":\"") + 9;
                            int end = body.indexOf("\"", start);
                            String b64 = body.substring(start, end);
                            String val = new String(Base64.getDecoder().decode(b64));
                            if (val.equals("DRAINING_TO_POSTGRES")) updateState(DbState.DRAINING_TO_POSTGRES);
                            else if (val.equals("POSTGRES")) updateState(DbState.POSTGRES);
                            else if (val.equals("ORACLE")) updateState(DbState.ORACLE);
                        }
                    } else if (res.statusCode() == 404) {
                        updateState(DbState.ORACLE);
                        Thread.sleep(5000);
                    }
                } catch (Exception e) {
                    try { Thread.sleep(2000); } catch (InterruptedException ie) { break; }
                }
            }
        });
    }

    private void updateState(DbState newState) {
        DbState old = currentState.getAndSet(newState);
        if (old != newState) {
            log.info("Transitioned DB State: " + old + " -> " + newState);
            synchronized (pauseLock) {
                pauseLock.notifyAll();
            }
        }
    }
}
