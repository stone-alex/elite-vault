package elite.vault.db.dao;

public final class SqlFragments {

    public static final String NEIGHBOR_SYSTEMS_CTE = """
            WITH origin AS (
                SELECT starName, systemAddress, x, y, z
                FROM star_system
                WHERE starName = :starName
                LIMIT 1
            ),
            neighbors AS (
                SELECT s2.systemAddress
                FROM origin o
                JOIN star_system s2
                  ON s2.starName <> o.starName
                WHERE s2.x BETWEEN o.x - :range AND o.x + :range
                  AND s2.y BETWEEN o.y - :range AND o.y + :range
                  AND s2.z BETWEEN o.z - :range AND o.z + :range
                  AND (
                        (s2.x - o.x) * (s2.x - o.x) +
                        (s2.y - o.y) * (s2.y - o.y) +
                        (s2.z - o.z) * (s2.z - o.z)
                      ) <= (:range * :range)
            )
            """;
}
