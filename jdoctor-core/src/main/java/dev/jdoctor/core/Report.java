package dev.jdoctor.core;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * The result every jdoctor module produces, whatever it diagnoses. Its JSON form is a public,
 * versioned contract described in {@code docs/report.schema.json}: other tools (CI test
 * libraries, the hosted platform) read and write it.
 *
 * @param schemaVersion version of this format; bumped only on breaking changes
 * @param kind          what was diagnosed, for example {@code jvm-diagnosis}
 * @param producer      tool and version that made the report
 * @param createdAt     when the report was made
 * @param subject       what the report is about, for example {@code pid:4242}
 * @param facts         short context lines, such as GC or heap size
 * @param findings      findings, most severe first
 * @param explanations  LLM explanations keyed by finding id
 */
public record Report(
        int schemaVersion,
        String kind,
        String producer,
        Instant createdAt,
        String subject,
        List<String> facts,
        List<Finding> findings,
        Map<String, String> explanations) {

    public static final int SCHEMA_VERSION = 1;

    /** Report kind for the JVM diagnosis of {@code jdoctor analyze}. */
    public static final String KIND_JVM = "jvm-diagnosis";

    public Report {
        facts = List.copyOf(facts);
        findings = List.copyOf(findings);
        explanations = Map.copyOf(explanations);
    }

    public static Report create(String kind, String subject, List<String> facts, List<Finding> findings,
            Map<String, String> explanations) {
        return new Report(SCHEMA_VERSION, kind, "jdoctor " + Version.current(), Instant.now(), subject, facts,
                findings, explanations);
    }
}
