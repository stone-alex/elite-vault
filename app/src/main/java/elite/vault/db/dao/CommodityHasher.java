package elite.vault.db.dao;

import java.util.List;

/**
 * Computes a lightweight hash of a commodity snapshot for change detection.
 * <p>
 * Purpose: EDDN delivers market snapshots whenever a commander docks.
 * If 4 commanders dock at the same station within a few seconds, the ingest
 * layer receives 4 identical snapshots — each would trigger a full
 * DELETE + bulk INSERT against the partitioned commodity table.
 * <p>
 * This hasher produces a long from the snapshot contents. The ingest layer
 * calls CommodityDao.isHashUnchanged before every replace cycle; if the hash
 * matches the stored value, the snapshot is discarded without touching the DB.
 * <p>
 * This is NOT cryptographic. Goals:
 * - Fast (single pass, no allocations)
 * - Sensitive to any price/stock/demand change
 * - Stable: same data always produces same hash regardless of row order
 * <p>
 * Algorithm: order-independent XOR of per-row hashes, where each row hash
 * mixes commodityId, buyPrice, sellPrice, stock, and demand using
 * multiplication by primes. XOR is order-independent so snapshot row ordering
 * from EDDN does not affect the result.
 */
public final class CommodityHasher {

    private CommodityHasher() {
    }

    /**
     * Compute a snapshot hash from a list of CommodityDao.CommodityRow.
     *
     * @param rows the incoming snapshot rows (any order)
     * @return a long hash; two snapshots with identical data produce the same value
     */
    public static long hash(List<CommodityDao.CommodityRow> rows) {
        long result = 0L;
        for (CommodityDao.CommodityRow row : rows) {
            result ^= rowHash(row);
        }
        return result;
    }

    private static long rowHash(CommodityDao.CommodityRow row) {
        // Mix fields using prime multipliers. The intent is that a change to
        // any single field produces a different row hash and therefore a
        // different snapshot hash. Perfect collision resistance is not required.
        long h = 17L;
        h = h * 31 + row.getCommodityId();
        h = h * 31 + row.getBuyPrice();
        h = h * 31 + row.getSellPrice();
        h = h * 31 + row.getStock();
        h = h * 31 + row.getDemand();
        return h;
    }
}