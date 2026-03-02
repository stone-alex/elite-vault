package elite.vault.db.util;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class TimeUtil {

    public static String toEntityDateTime(ZonedDateTime ts) {
        if (ts == null) ts = ZonedDateTime.now();
        return ts.format(DateTimeFormatter.ISO_INSTANT);
    }

    public static ZonedDateTime toDtoDateTime(String date) {
        if (date == null) return ZonedDateTime.now();
        return ZonedDateTime.parse(date, DateTimeFormatter.ISO_INSTANT);
    }
}
