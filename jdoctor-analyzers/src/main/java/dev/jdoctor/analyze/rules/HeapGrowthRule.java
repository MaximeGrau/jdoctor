package dev.jdoctor.analyze.rules;

import dev.jdoctor.analyze.Diagnosis;
import dev.jdoctor.analyze.Rule;
import dev.jdoctor.analyze.Units;
import dev.jdoctor.analyze.parse.HeapHistogram;
import dev.jdoctor.analyze.parse.JfrSummary;
import dev.jdoctor.core.Finding;
import dev.jdoctor.core.Severity;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.OptionalLong;

/**
 * JD-MEM-001: the heap floor after GC keeps rising, the classic sign of a memory leak.
 *
 * <p>Young collections leave old-generation garbage behind, so raw after-GC values are noisy.
 * The rule splits the recording into windows, keeps the lowest after-GC value of each window
 * (the floor), and fits a line through those floors.
 */
public final class HeapGrowthRule implements Rule {

    public static final String ID = "JD-MEM-001";

    static final int WINDOWS = 6;
    static final int MIN_WINDOWS_WITH_DATA = 4;
    static final Duration MIN_RECORDING = Duration.ofSeconds(20);
    static final double MIN_R_SQUARED = 0.6;
    static final long MIN_GROWTH_PER_MINUTE = 5L << 20;
    static final double CRITICAL_MINUTES_TO_FULL = 60;

    @Override
    public List<Finding> evaluate(Diagnosis diagnosis) {
        if (diagnosis.jfr().isEmpty()) {
            return List.of();
        }
        JfrSummary jfr = diagnosis.jfr().get();
        if (jfr.duration().compareTo(MIN_RECORDING) < 0 || jfr.heapAfterGc().size() < MIN_WINDOWS_WITH_DATA) {
            return List.of();
        }

        List<double[]> floors = floors(jfr);
        if (floors.size() < MIN_WINDOWS_WITH_DATA) {
            return List.of();
        }
        Fit fit = Fit.of(floors);
        double perMinute = fit.slope() * 60;
        if (fit.rSquared() < MIN_R_SQUARED || perMinute < MIN_GROWTH_PER_MINUTE) {
            return List.of();
        }

        long first = (long) floors.get(0)[1];
        long last = (long) floors.get(floors.size() - 1)[1];
        List<String> evidence = new ArrayList<>();
        evidence.add("Heap floor after GC rose from " + Units.bytes(first) + " to " + Units.bytes(last)
                + " over " + jfr.duration().toSeconds() + " s (" + jfr.heapAfterGc().size() + " collections)");
        evidence.add(String.format(Locale.ROOT, "Growth: %s/min, linear fit r² = %.2f",
                Units.bytes((long) perMinute), fit.rSquared()));

        Severity severity = Severity.WARNING;
        OptionalLong maxHeap = diagnosis.maxHeapBytes();
        if (maxHeap.isPresent()) {
            double minutesToFull = (maxHeap.getAsLong() - last) / perMinute;
            evidence.add(String.format(Locale.ROOT, "Max heap %s: full in about %.0f min at this rate",
                    Units.bytes(maxHeap.getAsLong()), Math.max(0, minutesToFull)));
            if (minutesToFull < CRITICAL_MINUTES_TO_FULL) {
                severity = Severity.CRITICAL;
            }
        }
        for (HeapHistogram.Entry entry : diagnosis.histogram().stream().limit(3).toList()) {
            evidence.add("Top retained class: " + entry.className() + " (" + entry.instances() + " instances, "
                    + Units.bytes(entry.bytes()) + ")");
        }

        return List.of(new Finding(ID, severity,
                "Heap after GC grows steadily (+" + Units.bytes((long) perMinute) + "/min): probable memory leak",
                evidence,
                "Look for collections that only grow: caches without eviction, static maps, listeners never"
                        + " removed. Take a heap dump (jcmd <pid> GC.heap_dump) and open it in Eclipse MAT to see"
                        + " what retains the top classes."));
    }

    /** Lowest after-GC value per window, as (seconds since start, bytes) pairs. */
    private static List<double[]> floors(JfrSummary jfr) {
        double total = jfr.duration().toMillis() / 1000.0;
        double[] min = new double[WINDOWS];
        double[] at = new double[WINDOWS];
        boolean[] seen = new boolean[WINDOWS];
        for (JfrSummary.HeapSample sample : jfr.heapAfterGc()) {
            double t = Duration.between(jfr.start(), sample.time()).toMillis() / 1000.0;
            int w = Math.min(WINDOWS - 1, Math.max(0, (int) (t / total * WINDOWS)));
            if (!seen[w] || sample.usedBytes() < min[w]) {
                min[w] = sample.usedBytes();
                at[w] = t;
                seen[w] = true;
            }
        }
        List<double[]> points = new ArrayList<>();
        for (int w = 0; w < WINDOWS; w++) {
            if (seen[w]) {
                points.add(new double[] {at[w], min[w]});
            }
        }
        return points;
    }

    /** Least-squares line through (x, y) points. */
    record Fit(double slope, double rSquared) {

        static Fit of(List<double[]> points) {
            int n = points.size();
            double sx = 0;
            double sy = 0;
            for (double[] p : points) {
                sx += p[0];
                sy += p[1];
            }
            double mx = sx / n;
            double my = sy / n;
            double sxx = 0;
            double sxy = 0;
            double syy = 0;
            for (double[] p : points) {
                sxx += (p[0] - mx) * (p[0] - mx);
                sxy += (p[0] - mx) * (p[1] - my);
                syy += (p[1] - my) * (p[1] - my);
            }
            if (sxx == 0 || syy == 0) {
                return new Fit(0, 0);
            }
            return new Fit(sxy / sxx, (sxy * sxy) / (sxx * syy));
        }
    }
}
