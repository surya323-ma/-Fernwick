package com.nursery;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;

/**
 * Runs once when the app starts. Creates the tables (CREATE TABLE IF NOT
 * EXISTS) and seeds the ten Fernwick products (INSERT IGNORE, keyed by
 * product id) so a fresh MySQL database is ready to use immediately -
 * no manual migration step required.
 */
@WebListener
public class DBInitializer implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent event) {
        String script;

        try (InputStream in = getClass().getClassLoader().getResourceAsStream("schema.sql")) {
            if (in == null) {
                System.err.println("[Nursery] schema.sql not found on the classpath - skipping auto setup.");
                return;
            }
            StringBuilder builder = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmedLine = line.trim();
                    if (trimmedLine.startsWith("--") || trimmedLine.isEmpty()) {
                        continue;
                    }
                    builder.append(line).append('\n');
                }
            }
            script = builder.toString();
        } catch (Exception e) {
            System.err.println("[Nursery] Could not read schema.sql:");
            e.printStackTrace();
            return;
        }

        try (Connection connection = DBConnection.getConnection();
             Statement statement = connection.createStatement()) {

            for (String rawStatement : script.split(";")) {
                String trimmed = rawStatement.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                statement.execute(trimmed);
            }
            System.out.println("[Nursery] Database schema ready.");

        } catch (Exception e) {
            System.err.println("[Nursery] Database auto-setup failed - check DB_URL / DB_USER / DB_PASSWORD:");
            e.printStackTrace();
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        // Nothing to release explicitly - connections are opened per-request.
    }
}
