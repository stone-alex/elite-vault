package elite.vault.util;


public class OsDetector {

    public static String os = System.getProperty("os.name").toLowerCase();

    public static OS getOs() {
        if (os.contains("linux")) return OS.LINUX;
        if (os.contains("mac") || os.contains("darwin")) return OS.MAC;
        return OS.WINDOWS;
    }

    public enum OS {
        WINDOWS("windows"), LINUX("linux"), MAC("mac");

        private String os;

        OS(String os) {
            this.os = os;
        }

        public String getOs() {
            return os;
        }
    }
}
