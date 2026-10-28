package dev.jdoctor.analyze;

import java.util.Locale;

/** Formatting helpers so every rule prints numbers the same way. */
public final class Units {

    private Units() {
    }

    public static String bytes(long bytes) {
        double abs = Math.abs(bytes);
        if (abs >= 1L << 30) {
            return String.format(Locale.ROOT, "%.1f GiB", bytes / (double) (1L << 30));
        }
        if (abs >= 1L << 20) {
            return String.format(Locale.ROOT, "%.0f MiB", bytes / (double) (1L << 20));
        }
        if (abs >= 1L << 10) {
            return String.format(Locale.ROOT, "%.0f KiB", bytes / (double) (1L << 10));
        }
        return bytes + " B";
    }

    public static String percent(double ratio) {
        return String.format(Locale.ROOT, "%.1f %%", ratio * 100);
    }

    public static String millis(double ms) {
        return String.format(Locale.ROOT, "%.0f ms", ms);
    }
}
