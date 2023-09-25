package bobo.utils;

import java.util.concurrent.TimeUnit;

public final class TimeFormat {
    private TimeFormat() {} // Prevent instantiation

    /**
     * Formats the given time in milliseconds to a string in the format HH:MM:SS.
     *
     * @param timeInMillis The time in milliseconds.
     * @return The formatted time.
     */
    public static String formatTime(long timeInMillis) {
        final long hours = timeInMillis / TimeUnit.HOURS.toMillis(1);
        timeInMillis %= TimeUnit.HOURS.toMillis(1);
        final long minutes = timeInMillis / TimeUnit.MINUTES.toMillis(1);
        timeInMillis %= TimeUnit.MINUTES.toMillis(1);
        final long seconds = timeInMillis / TimeUnit.SECONDS.toMillis(1);

        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}