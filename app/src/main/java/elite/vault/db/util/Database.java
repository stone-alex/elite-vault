package elite.vault.db.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import elite.vault.util.AppPaths;
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
import java.util.function.Function;

public class Database {

    private static final Logger log = LogManager.getLogger(Database.class);

    private static final HikariDataSource DATA_SOURCE;
    private static final Jdbi JDBI;

    private static void migrateIfNeeded() {
        JDBI.useHandle(handle -> {
            try {
                DatabaseMigrator.migrate(handle);
            } catch (Exception e) {
                throw new RuntimeException("Migration failed - DB may be corrupt", e);
            }
        });
    }

    /**
     * Standard single-DAO operation. Opens a handle, runs the block, closes it.
     * Not transactional - do not use when you need DELETE + INSERT atomicity.
     */
    public static <T, R> R withDao(Class<T> daoClass, Function<T, R> block) {
        try {
            return JDBI.withExtension(daoClass, block::apply);
        } catch (Exception e) {
            throw new RuntimeException("DAO operation failed: " + daoClass.getSimpleName(), e);
        }
    }

    /**
     * Transactional handle operation. Opens a single connection, begins a transaction,
     * runs the block, commits on success, rolls back on any exception.
     *
     * Use this when you need multiple DAO operations to be atomic - e.g. the
     * commodity snapshot replace (DELETE existing rows + bulk INSERT new rows).
     */
    public static <R> R withTransaction(Function<Handle, R> block) {
        try {
            return JDBI.inTransaction(handle -> block.apply(handle));
        } catch (Exception e) {
            throw new RuntimeException("Transactional operation failed", e);
        }
    }

    /**
     * Raw handle query - opens a handle, runs the block, closes it.
     * Use this when you need Handle-level features (e.g. defineList for dynamic IN-clauses).
     * Not transactional - use withTransaction if you need atomicity.
     */
    public static <R> R withHandle(Function<Handle, R> block) {
        try {
            return JDBI.withHandle(handle -> block.apply(handle));
        } catch (Exception e) {
            throw new RuntimeException("Handle operation failed", e);
        }
    }

    /**
     * Raw handle - caller is responsible for close(). Use sparingly.
     */
    public static Handle init() {
        return JDBI.open();
    }

    public static void shutdown() {
        if (DATA_SOURCE != null && !DATA_SOURCE.isClosed()) {
            DATA_SOURCE.close();
        }
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
                                    // skip
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
                                        // skip
                                    }
                                });
                    }
                }
            }
        }
        return classes;
    }

    static {
        try {
            Path dbPath = AppPaths.getDatabasePath();
            log.info("SQLite database: {}", dbPath);

            String url = "jdbc:sqlite:" + dbPath
                    + "?journal_mode=WAL"
                    + "&busy_timeout=5000"
                    + "&synchronous=NORMAL"
                    + "&foreign_keys=ON";

            HikariConfig hikari = new HikariConfig();
            hikari.setJdbcUrl(url);
            hikari.setMaximumPoolSize(4);
            hikari.setMinimumIdle(1);
            hikari.setConnectionTimeout(10_000);
            hikari.setIdleTimeout(300_000);
            hikari.setMaxLifetime(600_000);
            hikari.setPoolName("EliteVaultPool");
            hikari.setConnectionTestQuery("SELECT 1");

            DATA_SOURCE = new HikariDataSource(hikari);
            JDBI = Jdbi.create(DATA_SOURCE).installPlugin(new SqlObjectPlugin());

            JDBI.withHandle(h -> {
                String version = h.createQuery("SELECT sqlite_version()").mapTo(String.class).one();
                log.info("Connected to SQLite {} - pool max={}", version, hikari.getMaximumPoolSize());

                h.execute("PRAGMA journal_mode = WAL;");
                h.execute("PRAGMA synchronous = NORMAL;");
                h.execute("PRAGMA busy_timeout = 5000;");
                h.execute("PRAGMA foreign_keys = ON;");
                h.execute("PRAGMA cache_size = -64000;");
                h.execute("PRAGMA temp_store = MEMORY;");
                h.execute("PRAGMA mmap_size = 268435456;");

                try {
                    Set<Class<?>> daoClasses = findDaoClasses("elite.vault.db.dao");
                    for (Class<?> daoClass : daoClasses) {
                        h.attach(daoClass);
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Failed to warm DAO classes", e);
                }

                return null;
            });

            migrateIfNeeded();

        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }
}
