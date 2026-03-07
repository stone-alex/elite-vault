package elite.vault.db.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
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
import java.util.function.Function;

public class Database {
    private static final Logger log = LogManager.getLogger(Database.class);

    private static final Jdbi JDBI;
    private static final HikariDataSource dataSource;
    private static final ConfigManager conf = ConfigManager.getInstance();

    private static void migrateIfNeeded() {
        JDBI.useHandle(handle -> {
            try {
                DatabaseMigrator.migrate(handle);
            } catch (Exception e) {
                throw new RuntimeException("Migration failed — your DB might be b0rked", e);
            }
        });
        try (Handle h = JDBI.open()) {
            h.execute("CALL maintain_commodity_partitions(72, 6)");
            log.info("Commodity partitions initialized");
        } catch (Exception e) {
            log.warn("Partition maintenance failed at startup — commodity inserts may deadlock", e);
        }
    }


    /**
     * Standard single-DAO operation. Opens a handle, runs the block, closes the handle.
     * Not transactional — do not use when you need DELETE + INSERT atomicity.
     */
    public static <T, R> R withDao(Class<T> daoClass, Function<T, R> block) {
        try {
            return JDBI.withExtension(daoClass, block::apply);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("DAO operation failed: " + daoClass.getSimpleName(), e);
        }
    }

    /**
     * Transactional handle operation. Opens a single connection, begins a transaction,
     * runs the block, commits on success, rolls back on any exception.
     * <p>
     * Use this when you need multiple DAO operations to be atomic — e.g. the
     * commodity snapshot replace (DELETE existing rows + bulk INSERT new rows).
     * <p>
     * Example:
     * <pre>
     *   Database.withTransaction(handle -> {
     *       CommodityDao dao = handle.attach(CommodityDao.class);
     *       dao.deleteByMarket(marketId);
     *       dao.insertBatch(batch);
     *       return null;
     *   });
     * </pre>
     */
    public static <R> R withTransaction(Function<Handle, R> block) {
        try {
            return JDBI.inTransaction(handle -> block.apply(handle));
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Transactional operation failed", e);
        }
    }

    /**
     * Raw handle — caller is responsible for close(). Use sparingly.
     */
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
        String host = conf.getSystemKey(ConfigManager.DB_SERVER);
        String port = conf.getSystemKey(ConfigManager.DB_PORT);
        String dbName = conf.getSystemKey(ConfigManager.DB_NAME);
        String user = conf.getSystemKey(ConfigManager.DB_USER);
        String pass = conf.getSystemKey(ConfigManager.DB_PASS);

        String jdbcUrl = String.format(
                "jdbc:mariadb://%s:%s/%s" +
                "?useUnicode=true" +
                "&characterEncoding=UTF-8" +
                "&connectionAttributes=program_name:EliteVault" +
                "&useServerPrepStmts=true" +
                "&cachePrepStmts=true" +
                "&prepStmtCacheSize=150" +
                "&prepStmtCacheSqlLimit=2048" +
                "&rewriteBatchedStatements=true" +   // multi-row INSERT rewrite — critical for batch perf
                "&socketTimeout=30000" +
                "&connectTimeout=10000" +
                "&useLocalSessionState=true" +
                "&useLocalTransactionState=true" +
                "&cacheResultSetMetadata=true" +
                "&maintainTimeStats=false" +
                "&useMysqlMetadata=true" +
                "&allowPublicKeyRetrieval=true",
                host, port, dbName
        );

        // Configure HikariCP connection pool
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(user);
        config.setPassword(pass);

        // Pool sizing for high-throughput EDDN ingest
        config.setMaximumPoolSize(20);              // Max connections
        config.setMinimumIdle(5);                   // Keep 5 warm connections
        config.setConnectionTimeout(10000);         // 10s to get connection from pool
        config.setIdleTimeout(300000);              // 5min idle before closing
        config.setMaxLifetime(600000);              // 10min max connection lifetime
        config.setKeepaliveTime(120000);            // 2min keepalive ping

        // Performance tuning
        config.setPoolName("EliteVaultPool");
        config.setAutoCommit(true);
        config.setLeakDetectionThreshold(60000);    // Warn if connection held > 60s

        dataSource = new HikariDataSource(config);

        JDBI = Jdbi.create(dataSource)
                .installPlugin(new SqlObjectPlugin());

        try (var h = JDBI.open()) {
            String version = h.createQuery("SELECT VERSION()").mapTo(String.class).one();
            log.info("Connected to MariaDB version: {} with HikariCP pool (max={}, min={})",
                    version, config.getMaximumPoolSize(), config.getMinimumIdle());
        } catch (Exception e) {
            throw new RuntimeException(
                    "MariaDB connection failed. Check host/port/credentials/database existence.\n" +
                    "URL was: " + jdbcUrl.replaceAll("(?<=password=)[^&]+", "****"), e);
        }

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