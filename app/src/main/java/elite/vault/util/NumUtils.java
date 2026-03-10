package elite.vault.util;

public class NumUtils {

    public static Integer getIntSafely(String s) {
        if (s == null) return 0;
        return Integer.parseInt(s);
    }

    public static Double getDoubleSafely(String s) {
        if (s == null || s.isEmpty()) return 0.0;
        return Double.parseDouble(s);
    }
}
