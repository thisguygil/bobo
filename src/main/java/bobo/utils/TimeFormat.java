package bobo.utils;

import javax.annotation.Nonnull;
import java.util.concurrent.TimeUnit;

public final class TimeFormat {
    private TimeFormat() {} // Prevent instantiation

    /**
     * Formats the given time in milliseconds to a string in the format HH:MM:SS, or MM:SS if the time is less than an hour.
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

        return hours > 0 ? String.format("%02d:%02d:%02d", hours, minutes, seconds) : String.format("%02d:%02d", minutes, seconds);
    }

    /**
     * Parses the given time string in the format HH:MM:SS to milliseconds.
     *
     * @param time The time string.
     * @return The time in milliseconds.
     */
    public static long parseTime(@Nonnull String time) {
        String[] split = time.split(":");
        if (split.length != 3) {
            return -1;
        }

        long hours;
        long minutes;
        long seconds;
        try {
            hours = Long.parseLong(split[0]);
            minutes = Long.parseLong(split[1]);
            seconds = Long.parseLong(split[2]);
        } catch (NumberFormatException e) {
            return -1;
        }

        return TimeUnit.HOURS.toMillis(hours) + TimeUnit.MINUTES.toMillis(minutes) + TimeUnit.SECONDS.toMillis(seconds);
    }
}