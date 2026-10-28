package dev.jdoctor.analyze.rules;

import dev.jdoctor.analyze.Diagnosis;
import dev.jdoctor.analyze.Frames;
import dev.jdoctor.analyze.Rule;
import dev.jdoctor.analyze.parse.ThreadDump;
import dev.jdoctor.core.Finding;
import dev.jdoctor.core.Severity;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * JD-SPR-001: request threads queue for a HikariCP connection in several thread dumps.
 *
 * <p>One dump can catch a short burst; the same queue in several dumps means the pool is
 * saturated. The rule also names the application code that asks for connections.
 */
public final class HikariPoolExhaustionRule implements Rule {

    public static final String ID = "JD-SPR-001";

    static final String GET_CONNECTION = "com.zaxxer.hikari.pool.HikariPool.getConnection";
    static final int MIN_WAITERS = 3;
    static final int CRITICAL_WAITERS = 20;

    @Override
    public List<Finding> evaluate(Diagnosis diagnosis) {
        List<ThreadDump> dumps = diagnosis.threadDumps();
        if (dumps.isEmpty()) {
            return List.of();
        }
        List<Integer> waitersPerDump = dumps.stream().map(d -> waiters(d).size()).toList();
        long saturatedDumps = waitersPerDump.stream().filter(n -> n >= MIN_WAITERS).count();
        if (saturatedDumps < Math.min(2, dumps.size())) {
            return List.of();
        }
        int maxWaiters = waitersPerDump.stream().mapToInt(Integer::intValue).max().orElse(0);

        List<String> evidence = new ArrayList<>();
        evidence.add("Threads waiting for a connection, per dump: "
                + waitersPerDump.stream().map(String::valueOf).collect(Collectors.joining(", ")));
        Map<String, Long> callers = dumps.stream()
                .flatMap(d -> waiters(d).stream())
                .map(HikariPoolExhaustionRule::applicationCaller)
                .flatMap(Optional::stream)
                .collect(Collectors.groupingBy(c -> c, Collectors.counting()));
        callers.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(3)
                .forEach(e -> evidence.add("Waiting from " + e.getKey() + " (" + e.getValue() + " times)"));

        Severity severity = maxWaiters >= CRITICAL_WAITERS ? Severity.CRITICAL : Severity.WARNING;
        return List.of(new Finding(ID, severity,
                "HikariCP pool saturated: up to " + maxWaiters + " threads waiting for a connection",
                evidence,
                "Find what holds connections for long: slow queries, or remote calls made inside @Transactional."
                        + " Set spring.datasource.hikari.leak-detection-threshold to log long holders. Raising"
                        + " maximum-pool-size only helps if the database can serve more connections."));
    }

    private static List<ThreadDump.ThreadInfo> waiters(ThreadDump dump) {
        return dump.threads().stream()
                .filter(t -> t.state().equals("WAITING") || t.state().equals("TIMED_WAITING"))
                .filter(t -> t.hasFrameStartingWith(GET_CONNECTION))
                .toList();
    }

    /** First frame that belongs to the application rather than a framework or the JDK. */
    static Optional<String> applicationCaller(ThreadDump.ThreadInfo thread) {
        return thread.frames().stream()
                .filter(Frames::isApplication)
                .map(Frames::method)
                .findFirst();
    }
}
