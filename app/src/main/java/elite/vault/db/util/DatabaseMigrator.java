package elite.vault.db.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdbi.v3.core.Handle;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;


public class DatabaseMigrator {

    private static final Logger log = LogManager.getLogger(DatabaseMigrator.class);
    private static final Pattern MIGRATION_PATTERN = Pattern.compile("^(\\d{1,6})(__.*)?\\.sql$");
    private static final String MIGRATIONS_PATH = "/db-migration";

    public static void migrate(Handle handle) throws Exception {
        handle.execute("""
                CREATE TABLE IF NOT EXISTS schema_migration (
                    version     VARCHAR(255) PRIMARY KEY,
                    applied_at  DATETIME DEFAULT CURRENT_TIMESTAMP
                ) ENGINE=InnoDB CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
                """);

        Set<String> allFiles = findMigrationFiles();

        var applied = handle.createQuery("SELECT version FROM schema_migration")
                .mapTo(String.class)
                .set();

        int countApplied = 0;

        for (String file : allFiles) {
            if (applied.contains(file)) continue;

            log.info("Applying migration: {}", file);

            try (var in = DatabaseMigrator.class.getResourceAsStream(MIGRATIONS_PATH + "/" + file)) {
                if (in == null) throw new IllegalStateException("Migration not found: " + file);

                String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8)
                        .replace("\r\n", "\n")
                        .trim();

                if (containsCompoundStatements(sql)) {
                    // File contains stored procedures or compound BEGIN...END blocks.
                    // JDBI's createScript() splits on semicolons and cannot handle these.
                    // Execute via raw JDBC instead, statements separated by ---
                    executeProcedureFile(handle, sql, file);
                } else {
                    // Plain DDL — safe to use JDBI's script executor
                    handle.createScript(sql).execute();
                }
            }

            handle.execute("INSERT INTO schema_migration (version) VALUES (?)", file);
            countApplied++;
        }

        log.info("Migrations finished. Newly applied: {} / total known: {}", countApplied, allFiles.size());
    }

    /**
     * Returns true if the SQL contains compound statements (BEGIN...END blocks)
     * that JDBI's semicolon-based script splitter cannot handle correctly.
     */
    private static boolean containsCompoundStatements(String sql) {
        // Simple heuristic: presence of BEGIN (case-insensitive, whole word) indicates
        // a stored procedure or compound statement body.
        return sql.toUpperCase().contains("\nBEGIN") || sql.toUpperCase().startsWith("BEGIN");
    }

    /**
     * Execute a file containing stored procedures or events via raw JDBC.
     * Statements in the file are separated by lines containing only "---".
     * Each statement is executed individually — no DELIMITER tricks needed.
     */
    private static void executeProcedureFile(Handle handle, String sql, String filename) throws Exception {
        // Split on /*SPLIT*/ marker, then strip comment lines per statement
        String[] statements = sql.split("/\\*SPLIT\\*/");

        Connection conn = handle.getConnection();
        for (String stmt : statements) {
            String trimmed = stmt.trim();
            // Remove comment lines
            trimmed = trimmed.replaceAll("(?m)^--[^\n]*\n?", "").trim();
            if (trimmed.isEmpty()) continue;

            log.debug("Executing procedure statement from {}: {}...",
                    filename, trimmed.substring(0, Math.min(60, trimmed.length())));

            try (Statement jdbcStmt = conn.createStatement()) {
                jdbcStmt.execute(trimmed);
            } catch (Exception e) {
                log.error("Failed executing statement from {}:\n{}", filename, trimmed, e);
                throw e;
            }
        }
    }

    private static Set<String> findMigrationFiles() throws IOException, URISyntaxException {
        Set<String> files = new TreeSet<>();
        ClassLoader cl = DatabaseMigrator.class.getClassLoader();
        String path = "db-migration";

        Enumeration<URL> urls = cl.getResources(path);
        while (urls.hasMoreElements()) {
            URL url = urls.nextElement();
            String protocol = url.getProtocol();

            if ("jar".equals(protocol)) {
                String urlStr = url.toString();
                int sep = urlStr.indexOf("!/");
                String jarPart = urlStr.substring(0, sep);
                URI jarUri = URI.create(jarPart);

                try (FileSystem fs = FileSystems.newFileSystem(jarUri, Collections.emptyMap())) {
                    Path root = fs.getPath("/" + path);
                    try (var walk = Files.walk(root)) {
                        walk.filter(Files::isRegularFile)
                                .map(p -> p.getFileName().toString())
                                .filter(n -> MIGRATION_PATTERN.matcher(n).matches())
                                .forEach(files::add);
                    }
                }
            } else if ("file".equals(protocol)) {
                Path dir = Paths.get(url.toURI());
                try (var walk = Files.walk(dir)) {
                    walk.filter(Files::isRegularFile)
                            .map(p -> p.getFileName().toString())
                            .filter(n -> MIGRATION_PATTERN.matcher(n).matches())
                            .forEach(files::add);
                }
            }
        }

        if (files.isEmpty()) {
            throw new IllegalStateException("No migration files found in classpath: db-migration");
        }
        return files;
    }
}