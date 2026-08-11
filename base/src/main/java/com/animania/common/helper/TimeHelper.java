package com.animania.common.helper;

/** Tick conversion constants and the human-readable formatter retained from 1.12. */
public final class TimeHelper {
    public static final int SECOND = 20;
    public static final int MINUTE = SECOND * 60;
    public static final int HOUR = MINUTE * 60;
    public static final int DAY = HOUR * 24;

    private TimeHelper() { }

    public static String getTime(int ticks) {
        int remaining = Math.max(0, ticks);
        int days = remaining / DAY;
        remaining %= DAY;
        int hours = remaining / HOUR;
        remaining %= HOUR;
        int minutes = remaining / MINUTE;
        remaining %= MINUTE;
        int seconds = remaining / SECOND;

        StringBuilder result = new StringBuilder();
        append(result, days, "Day");
        append(result, hours, "Hour");
        append(result, minutes, "Minute");
        append(result, seconds, "Second");
        return result.toString();
    }

    private static void append(StringBuilder result, int amount, String unit) {
        if (amount <= 0) return;
        if (!result.isEmpty()) result.append(", ");
        result.append(amount).append(' ').append(unit);
        if (amount > 1) result.append('s');
    }
}
