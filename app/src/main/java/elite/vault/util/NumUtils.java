package elite.vault.util;

public class NumUtils {

    public static Integer getIntSafely(String s) {
        if (s == null) return 0;
        return Integer.parseInt(s);
    }
}
