package io.github.danhjalmberg.dronephotoservice.support;

import java.time.Duration;

/**
 * Formats simulation durations for presentation and diagnostic text.
 */
public final class TimeUtils {

    /**
     * Prevents instantiation of this utility class.
     */
    private TimeUtils() {
    }

    /**
     * Formats whole elapsed seconds as {@code HH:mm:ss}.
     *
     * <p>Subsecond precision is discarded, hours are not capped at 24, and
     * {@code null} produces an empty string. Simulation durations are expected to
     * be non-negative.</p>
     *
     * @param time elapsed simulation duration, or {@code null}
     * @return formatted duration, or an empty string for {@code null}
     */
    public static String formatSimulationTime(Duration time) {

        if (time == null) {
            return "";
        }

        long seconds = time.getSeconds();
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long remainingSeconds = seconds % 60;

        return String.format("%02d:%02d:%02d",
                hours,
                minutes,
                remainingSeconds);
    }
}
