package com.nursery;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Central place that opens JDBC connections.
 *
 * With no configuration at all, the app runs against an embedded,
 * in-memory H2 database (zero install, data resets each restart) - handy
 * for trying the project out or for local development.
 *
 * For MySQL (recommended for anything beyond local testing), set:
 *   DB_URL=jdbc:mysql://host:3306/nursery?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
 *   DB_USER=...
 *   DB_PASSWORD=...
 * The driver is picked automatically based on the DB_URL prefix.
 */
public final class DBConnection {

    private static final String DEFAULT_H2_URL =
            "jdbc:h2:mem:nursery;DB_CLOSE_DELAY=-1;MODE=MySQL;DATABASE_TO_LOWER=TRUE";

    private static final String URL;
    private static final String USER;
    private static final String PASSWORD;

    static {
        URL = System.getenv().getOrDefault("DB_URL", DEFAULT_H2_URL);
        USER = System.getenv().getOrDefault("DB_USER", URL.startsWith("jdbc:h2:") ? "sa" : "root");
        PASSWORD = System.getenv().getOrDefault("DB_PASSWORD", "");

        try {
            if (URL.startsWith("jdbc:h2:")) {
                Class.forName("org.h2.Driver");
                if (URL == DEFAULT_H2_URL) {
                    System.out.println("[Nursery] No DB_URL set - using an embedded in-memory H2 database. "
                            + "Data will reset on restart. Set DB_URL/DB_USER/DB_PASSWORD to use MySQL instead.");
                }
            } else {
                Class.forName("com.mysql.cj.jdbc.Driver");
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("JDBC driver not found on the classpath", e);
        }
    }

    private DBConnection() {
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
