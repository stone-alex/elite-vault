package elite.vault.util;

import java.util.List;

/**
 * Bubble filter - rejects EDDN events from systems outside the inhabited bubble.
 * <p>
 * The bubble is defined as a sphere of 500 ly radius centred on Sol (0, 0, 0)
 * in Elite Dangerous Cartesian coordinates. This covers ~16 000 inhabited systems
 * and ~24 000 market stations while excluding the 500 M+ uninhabited galaxy.
 * <p>
 * Squared-distance comparison avoids a sqrt on every inbound EDDN message.
 */
public final class BubbleFilter {

    private static final double BUBBLE_RADIUS_LY = 500.0;
    private static final double BUBBLE_RADIUS_SQ = BUBBLE_RADIUS_LY * BUBBLE_RADIUS_LY;  // 250 000

    private BubbleFilter() {
    }

    /**
     * Returns {@code true} if the StarPos list [x, y, z] is within 500 ly of Sol.
     * Returns {@code false} if the list is null or has fewer than 3 elements.
     */
    public static boolean isInBubble(List<Double> starPos) {
        if (starPos == null || starPos.size() < 3) return false;
        double x = starPos.get(0);
        double y = starPos.get(1);
        double z = starPos.get(2);
        return x * x + y * y + z * z <= BUBBLE_RADIUS_SQ;
    }
}
