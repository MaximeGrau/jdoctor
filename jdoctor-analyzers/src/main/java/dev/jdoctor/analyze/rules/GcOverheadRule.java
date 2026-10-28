package dev.jdoctor.analyze.rules;

import dev.jdoctor.analyze.Diagnosis;
import dev.jdoctor.analyze.Rule;
import dev.jdoctor.analyze.Units;
import dev.jdoctor.analyze.parse.JfrSummary;
import dev.jdoctor.core.Finding;
import dev.jdoctor.core.Severity;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** JD-GC-001: the application spends too much time stopped by the garbage collector. */
public final class GcOverheadRule implements Rule {

    public static final String ID = "JD-GC-001";

    static final Duration MIN_RECORDING = Duration.ofSeconds(10);
    static final double WARNING_RATIO = 0.05;
    static final double CRITICAL_RATIO = 0.15;
    static final Duration LONG_PAUSE = Duration.ofMillis(500);

    @Override
    public List<Finding> evaluate(Diagnosis diagnosis) {
        if (diagnosis.jfr().isEmpty()) {
            return List.of();
        }
        JfrSummary jfr = diagnosis.jfr().get();
        if (jfr.duration().compareTo(MIN_RECORDING) < 0 || jfr.collections().isEmpty()) {
            return List.of();
        }

        Duration paused = jfr.collections().stream()
                .map(JfrSummary.GcEvent::sumOfPauses)
                .reduce(Duration.ZERO, Duration::plus);
        double ratio = paused.toNanos() / (double) jfr.duration().toNanos();
        JfrSummary.GcEvent longest = jfr.collections().stream()
                .max(Comparator.comparing(JfrSummary.GcEvent::longestPause))
                .orElseThrow();

        Severity severity;
        if (ratio >= CRITICAL_RATIO) {
            severity = Severity.CRITICAL;
        } else if (ratio >= WARNING_RATIO || longest.longestPause().compareTo(LONG_PAUSE) >= 0) {
            severity = Severity.WARNING;
        } else {
            return List.of();
        }

        List<String> evidence = new ArrayList<>();
        evidence.add(jfr.collections().size() + " collections in " + jfr.duration().toSeconds() + " s, "
                + Units.millis(paused.toNanos() / 1e6) + " paused (" + Units.percent(ratio) + " of the time)");
        evidence.add("Longest pause: " + Units.millis(longest.longestPause().toNanos() / 1e6) + " ("
                + longest.name() + ", cause: " + longest.cause() + ")");
        Map<String, Long> causes = jfr.collections().stream()
                .collect(Collectors.groupingBy(JfrSummary.GcEvent::cause, Collectors.counting()));
        causes.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(3)
                .forEach(e -> evidence.add("Cause \"" + e.getKey() + "\": " + e.getValue() + " collections"));

        String topCause = causes.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("");
        String recommendation = topCause.equals("System.gc()")
                ? "Most collections are explicit System.gc() calls. Find and remove them, or start the JVM with"
                        + " -XX:+DisableExplicitGC if they come from a library."
                : "Check whether the heap is too small for the live data (see JD-MEM-001) or the allocation rate"
                        + " is too high. Profile allocations with the JFR recording in JDK Mission Control before"
                        + " tuning GC flags.";
        return List.of(new Finding(ID, severity,
                "GC pauses take " + Units.percent(ratio) + " of the time", evidence, recommendation));
    }

}
