package dev.jdoctor.analyze.rules;

import dev.jdoctor.analyze.Diagnosis;
import dev.jdoctor.analyze.Frames;
import dev.jdoctor.analyze.Rule;
import dev.jdoctor.analyze.parse.LogSummary;
import dev.jdoctor.core.Finding;
import dev.jdoctor.core.Severity;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JD-START-*: the Spring application did not start.
 *
 * <p>Spring Boot already explains most startup failures in its "APPLICATION FAILED TO START"
 * report. This rule recognizes the common cases, gives each its own id and runbook, and keeps
 * Spring's own description as evidence. A crash that Spring did not analyze (an exception in a
 * bean's constructor or {@code @PostConstruct}) is reported with its root cause and the
 * application line that threw it.
 */
public final class StartupFailureRule implements Rule {

    public static final String CRASH = "JD-START-001";
    public static final String DUPLICATE_BEAN = "JD-START-002";
    public static final String SEVERAL_CANDIDATES = "JD-START-003";
    public static final String MISSING_BEAN = "JD-START-004";
    public static final String CYCLE = "JD-START-005";
    public static final String PORT_IN_USE = "JD-START-006";

    static final int MAX_DESCRIPTION_LINES = 10;

    private record Kind(String id, String title, String recommendation) {
    }

    @Override
    public List<Finding> evaluate(Diagnosis diagnosis) {
        if (diagnosis.log().isEmpty()) {
            return List.of();
        }
        LogSummary log = diagnosis.log().get();
        Optional<LogSummary.LoggedException> crash = log.exceptions().stream()
                .filter(LogSummary.LoggedException::duringStartup)
                .reduce((first, second) -> second);
        if (log.startupFailure().isPresent()) {
            return List.of(analyzed(log.startupFailure().get(), crash));
        }
        return crash.map(StartupFailureRule::crashed).map(List::of).orElse(List.of());
    }

    private static Finding analyzed(LogSummary.StartupFailure failure, Optional<LogSummary.LoggedException> crash) {
        Kind kind = classify(failure.description(), crash);
        List<String> evidence = new ArrayList<>();
        evidence.add("Spring Boot reported: APPLICATION FAILED TO START");
        failure.description().lines()
                .filter(line -> !line.isBlank())
                .limit(MAX_DESCRIPTION_LINES)
                .map(String::strip)
                .forEach(evidence::add);
        String recommendation = kind.recommendation()
                + (failure.action().isEmpty() ? "" : " Spring Boot suggests: " + oneLine(failure.action()));
        return new Finding(kind.id(), Severity.CRITICAL, kind.title(), evidence, recommendation);
    }

    private static Finding crashed(LogSummary.LoggedException exception) {
        LogSummary.Cause root = exception.rootCause();
        List<String> evidence = new ArrayList<>();
        for (LogSummary.Cause cause : exception.chain()) {
            String prefix = cause == exception.chain().get(0) ? "" : "Caused by ";
            evidence.add(prefix + cause.simpleType() + (cause.message().isEmpty() ? "" : ": " + cause.message()));
        }
        Optional<String> where = root.frames().stream().filter(Frames::isApplication).findFirst();
        where.ifPresent(frame -> evidence.add("Thrown at " + frame));
        String title = "Application failed to start: " + root.simpleType()
                + where.map(f -> " in " + Frames.shortMethod(f)).orElse("");
        return new Finding(CRASH, Severity.CRITICAL, title, evidence,
                "Fix the root cause at the line shown: the outer exceptions only say which bean Spring was"
                        + " creating. Code in constructors and @PostConstruct runs at startup, so any"
                        + " missing setting or null value there stops the whole application.");
    }

    private static Kind classify(String description, Optional<LogSummary.LoggedException> crash) {
        String root = crash.map(e -> e.rootCause().type()).orElse("");
        if (description.contains("could not be registered. A bean with that name has already been defined")
                || root.endsWith("BeanDefinitionOverrideException")) {
            return new Kind(DUPLICATE_BEAN, "Two beans have the same name",
                    "Rename one of the @Bean methods or components, or remove the duplicate configuration."
                            + " Enabling bean overriding hides the conflict instead of fixing it.");
        }
        if (description.contains("required a single bean, but") || root.endsWith("NoUniqueBeanDefinitionException")) {
            return new Kind(SEVERAL_CANDIDATES, "Several beans match one injection point",
                    "Choose the bean explicitly: @Qualifier on the injection point, or @Primary on the"
                            + " default bean. Inject a List if the code needs all of them.");
        }
        if (description.contains("that could not be found") || root.endsWith("NoSuchBeanDefinitionException")) {
            return new Kind(MISSING_BEAN, "A required bean does not exist",
                    "Create the missing bean, or check why it is not created: missing @Component, a package"
                            + " outside component scanning, a disabled @Profile or @Conditional, or a missing starter.");
        }
        if (description.contains("form a cycle") || root.endsWith("BeanCurrentlyInCreationException")) {
            return new Kind(CYCLE, "Beans depend on each other in a cycle",
                    "Break the cycle: move the shared logic into a third bean, or publish an event instead of"
                            + " calling back. Avoid spring.main.allow-circular-references, which hides the design problem.");
        }
        if (description.contains("was already in use") || root.endsWith("PortInUseException")) {
            return new Kind(PORT_IN_USE, "The web server port is already in use",
                    "Another process listens on the port: often a previous instance still running."
                            + " Stop it or set server.port.");
        }
        return new Kind(CRASH, "Application failed to start",
                "Read Spring Boot's description below; it names the bean or property at fault.");
    }

    /** Joins Spring's lines, which are whole sentences, ending each one that has no punctuation. */
    static String oneLine(String text) {
        return text.lines()
                .map(String::strip)
                .filter(l -> !l.isEmpty())
                .map(l -> l.matches(".*[.!?:)]$") ? l : l + ".")
                .reduce((a, b) -> a + " " + b)
                .orElse("");
    }
}
