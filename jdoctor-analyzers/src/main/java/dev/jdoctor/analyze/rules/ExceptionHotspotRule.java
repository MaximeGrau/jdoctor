package dev.jdoctor.analyze.rules;

import dev.jdoctor.analyze.Diagnosis;
import dev.jdoctor.analyze.Frames;
import dev.jdoctor.analyze.Rule;
import dev.jdoctor.analyze.parse.LogSummary;
import dev.jdoctor.core.Finding;
import dev.jdoctor.core.Severity;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * JD-EXC-001 and JD-EXC-002: exceptions logged at runtime, grouped by type and by the application
 * line that threw them.
 *
 * <p>JDK and framework exceptions thrown from application code (NullPointerException,
 * IllegalStateException...) are bugs: JD-EXC-001. Exceptions defined by the application itself
 * (OrderNotFoundException...) are usually business errors that escaped without being handled:
 * JD-EXC-002, reported separately so that they do not hide the bugs.
 */
public final class ExceptionHotspotRule implements Rule {

    public static final String BUGS = "JD-EXC-001";
    public static final String UNHANDLED_APPLICATION = "JD-EXC-002";

    static final int MAX_GROUPS = 5;
    static final int CRITICAL_OCCURRENCES = 50;

    /** One kind of exception thrown from one place. */
    record Group(String type, String frame, String message, int count) {
    }

    @Override
    public List<Finding> evaluate(Diagnosis diagnosis) {
        if (diagnosis.log().isEmpty()) {
            return List.of();
        }
        List<Group> groups = groups(diagnosis.log().get());
        List<Group> bugs = groups.stream().filter(g -> !isApplicationType(g.type())).toList();
        List<Group> application = groups.stream().filter(g -> isApplicationType(g.type())).toList();

        List<Finding> findings = new ArrayList<>();
        if (!bugs.isEmpty()) {
            Group top = bugs.get(0);
            boolean npe = top.type().equals("java.lang.NullPointerException");
            findings.add(new Finding(BUGS,
                    bugs.stream().anyMatch(g -> g.count() >= CRITICAL_OCCURRENCES) ? Severity.CRITICAL : Severity.WARNING,
                    simple(top.type()) + " in " + Frames.shortMethod(top.frame()) + " (" + times(top.count()) + ")"
                            + (bugs.size() > 1 ? " and " + (bugs.size() - 1) + " other exception sites" : ""),
                    evidence(bugs),
                    npe
                            ? "The JDK message names the null value. Decide whether null is legal there: if not, fail"
                                    + " earlier with a clear error (validation, Optional.orElseThrow); if it is,"
                                    + " handle it (404 for a missing entity, a default value)."
                            : "Each line is a bug in application code, thrown from the place shown. Fix the most"
                                    + " frequent first."));
        }
        if (!application.isEmpty()) {
            Group top = application.get(0);
            findings.add(new Finding(UNHANDLED_APPLICATION, Severity.WARNING,
                    "Application exception " + simple(top.type()) + " escapes unhandled (" + times(top.count()) + ")",
                    evidence(application),
                    "These exceptions are defined by the application but reach the framework, which logs them"
                            + " as errors and answers HTTP 500. Map them to a status with @ResponseStatus or a"
                            + " @RestControllerAdvice handler, or catch them where the business case is handled."));
        }
        return findings;
    }

    /** Groups runtime exceptions by root cause type and application frame, most frequent first. */
    static List<Group> groups(LogSummary log) {
        Map<String, Group> byKey = new LinkedHashMap<>();
        for (LogSummary.LoggedException exception : log.exceptions()) {
            if (exception.duringStartup()) {
                continue;
            }
            LogSummary.Cause root = exception.rootCause();
            Optional<String> frame = applicationFrame(exception);
            if (frame.isEmpty()) {
                continue;
            }
            String key = root.type() + "@" + frame.get();
            Group previous = byKey.get(key);
            byKey.put(key, new Group(root.type(), frame.get(), root.message(),
                    previous == null ? 1 : previous.count() + 1));
        }
        return byKey.values().stream().sorted(Comparator.comparingInt(Group::count).reversed()).toList();
    }

    /** First application frame of the root cause, else of the closest wrapping exception. */
    private static Optional<String> applicationFrame(LogSummary.LoggedException exception) {
        List<LogSummary.Cause> chain = exception.chain();
        for (int i = chain.size() - 1; i >= 0; i--) {
            Optional<String> frame = chain.get(i).frames().stream().filter(Frames::isApplication).findFirst();
            if (frame.isPresent()) {
                return frame.map(Frames::withoutProxy);
            }
        }
        return Optional.empty();
    }

    private static List<String> evidence(List<Group> groups) {
        List<String> lines = new ArrayList<>();
        for (Group g : groups.stream().limit(MAX_GROUPS).toList()) {
            lines.add(times(g.count()) + " " + simple(g.type()) + " at " + g.frame()
                    + (g.message().isEmpty() ? "" : ": " + g.message()));
        }
        if (groups.size() > MAX_GROUPS) {
            lines.add("... and " + (groups.size() - MAX_GROUPS) + " other exception sites");
        }
        return lines;
    }

    private static boolean isApplicationType(String type) {
        return Frames.isApplication(type);
    }

    private static String simple(String type) {
        return type.substring(type.lastIndexOf('.') + 1);
    }

    private static String times(int count) {
        return count == 1 ? "1 time" : count + " times";
    }
}
