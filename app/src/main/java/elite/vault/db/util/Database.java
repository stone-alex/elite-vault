package elite.vault.db.util;

import elite.vault.ConfigManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;

import java.net.URI;
import java.net.URL;
import java.nio.file.*;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;


public class Database {
    private static final Logger log = LogManager.getLogger(Database.class);


    private static final Jdbi JDBI;
    private static final ConfigManager conf = ConfigManager.getInstance();

    private static void migrateIfNeeded() {
        JDBI.useHandle(handle -> {
            try {
                DatabaseMigrator.migrate(handle);
            } catch (Exception e) {
                throw new RuntimeException("Migration failed — your DB might be b0rked", e);
            }
        });
    }

    public static <T, R> R withDao(Class<T> daoClass, java.util.function.Function<T, R> block) {
        // Use withExtension to obtain a thread-safe handle for this specific operation
        try {
            return JDBI.withExtension(daoClass, block::apply);
        } catch (Exception e) {
            throw new RuntimeException("DAO operation failed: " + daoClass.getSimpleName(), e);
        }
    }

    // shortcut if you just need a handle
    public static Handle init() {
        return JDBI.open();
    }

    private static Set<Class<?>> findDaoClasses(String packageName) throws Exception {
        Set<Class<?>> classes = new HashSet<>();
        String path = packageName.replace('.', '/');
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        Enumeration<URL> resources = classLoader.getResources(path);
        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            String protocol = resource.getProtocol();

            if ("file".equals(protocol)) {
                Path root = Paths.get(resource.toURI());
                try (var walk = Files.walk(root)) {
                    walk.filter(p -> p.toString().endsWith(".class"))
                            .forEach(p -> {
                                try {
                                    String className = packageName + "." +
                                            root.relativize(p).toString()
                                                    .replace(FileSystems.getDefault().getSeparator(), ".")
                                                    .replace(".class", "");
                                    Class<?> clazz = Class.forName(className);
                                    if (clazz.isInterface() && clazz.getSimpleName().endsWith("Dao")) {
                                        classes.add(clazz);
                                    }
                                } catch (ClassNotFoundException e) {
                                    // Skip classes that can't be loaded
                                }
                            });
                }
            } else if ("jar".equals(protocol)) {
                String urlStr = resource.toString();
                int sep = urlStr.indexOf("!/");
                String jarPart = urlStr.substring(0, sep);
                URI jarUri = URI.create(jarPart);

                try (FileSystem fs = FileSystems.newFileSystem(jarUri, Collections.emptyMap())) {
                    Path root = fs.getPath("/" + path);
                    try (var walk = Files.walk(root)) {
                        walk.filter(p -> p.toString().endsWith(".class"))
                                .forEach(p -> {
                                    try {
                                        String className = packageName + "." +
                                                root.relativize(p).toString()
                                                        .replace("/", ".")
                                                        .replace(".class", "");
                                        Class<?> clazz = Class.forName(className);
                                        if (clazz.isInterface() && clazz.getSimpleName().endsWith("Dao")) {
                                            classes.add(clazz);
                                        }
                                    } catch (ClassNotFoundException e) {
                                        // Skip classes that can't be loaded
                                    }
                                });
                    }
                }
            }
        }
        return classes;
    }

    static {
        String host = conf.getSystemKey(ConfigManager.DB_SERVER);     // e.g. "127.0.0.1" or "db.example.com"
        String port = conf.getSystemKey(ConfigManager.DB_PORT);       // usually "3306"
        String dbName = conf.getSystemKey(ConfigManager.DB_NAME);
        String user = conf.getSystemKey(ConfigManager.DB_USER);
        String pass = conf.getSystemKey(ConfigManager.DB_PASS);

        // 2025–2026 recommended MariaDB JDBC URL for good ingest performance
        String jdbcUrl = String.format(
                "jdbc:mariadb://%s:%s/%s" +
                        "?useUnicode=true" +
                        "&characterEncoding=UTF-8" +
                        "&connectionAttributes=program_name:EliteVault" +           // nice for server monitoring
                        "&useServerPrepStmts=true" +                                // binary protocol (good for repeated queries)
                        "&cachePrepStmts=true" +
                        "&prepStmtCacheSize=300" +
                        "&prepStmtCacheSqlLimit=4096" +
                        "&rewriteBatchedStatements=true" +                          // critical: turns many INSERTs into one multi-row INSERT
                        "&socketTimeout=45000" +                                    // 45 seconds
                        "&connectTimeout=15000" +
                        "&useLocalSessionState=true" +
                        "&allowPublicKeyRetrieval=true",                            // only if needed for password auth
                host, port, dbName
        );

        JDBI = Jdbi.create(jdbcUrl, user, pass)
                .installPlugin(new SqlObjectPlugin());

        // Early connection validation + log basic info
        try (var h = JDBI.open()) {
            String version = h.createQuery("SELECT VERSION()").mapTo(String.class).one();
            log.info("Connected to MariaDB version: {}", version);
        } catch (Exception e) {
            throw new RuntimeException(
                    "MariaDB connection failed. Check host/port/credentials/database existence.\n" +
                            "URL was: " + jdbcUrl.replaceAll("(?<=password=)[^&]+", "****"), e);
        }

        // Attach all DAOs from the package (unchanged logic)
        JDBI.withHandle(h -> {
            try {
                Set<Class<?>> daoClasses = findDaoClasses("elite.vault.db.dao");
                for (Class<?> daoClass : daoClasses) {
                    h.attach(daoClass);
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to attach DAO classes", e);
            }
            return null;
        });

        migrateIfNeeded();
    }
}
