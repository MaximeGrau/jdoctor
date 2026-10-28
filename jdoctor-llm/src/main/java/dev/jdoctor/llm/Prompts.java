package dev.jdoctor.llm;

import dev.jdoctor.core.Finding;
import dev.jdoctor.core.Runbooks;

/** Builds the prompts sent to the LLM. Kept separate so they can be reviewed and tested. */
final class Prompts {

    static final String SYSTEM = """
            You are jdoctor, an expert in JVM and Spring Boot production troubleshooting.
            You receive one finding that a deterministic rule already proved, with its measured evidence
            and the project's runbook for that finding.
            Explain the most likely root cause and give concrete, ordered steps to confirm and fix it
            in a Spring Boot application: configuration properties, JVM flags or code patterns.
            Only rely on the evidence given. When the evidence does not settle a point, say what to
            collect to settle it. Do not invent numbers.
            Answer in plain Markdown, under 300 words, with the sections: Cause, Confirm, Fix.
            """;

    private Prompts() {
    }

    static String user(Finding finding, ExplainContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Finding ").append(finding.id()).append(" (").append(finding.severity()).append(")\n");
        sb.append(finding.title()).append("\n\n## Evidence\n");
        finding.evidence().forEach(e -> sb.append("- ").append(e).append('\n'));
        sb.append("\n## Default recommendation\n").append(finding.recommendation()).append('\n');
        if (!context.jvmSummary().isEmpty()) {
            sb.append("\n## JVM\n");
            context.jvmSummary().forEach(e -> sb.append("- ").append(e).append('\n'));
        }
        if (!context.otherFindings().isEmpty()) {
            sb.append("\n## Other findings in the same diagnosis\n");
            context.otherFindings().forEach(e -> sb.append("- ").append(e).append('\n'));
        }
        Runbooks.forFinding(finding.id())
                .ifPresent(runbook -> sb.append("\n## Runbook\n").append(runbook).append('\n'));
        return Redactor.redact(sb.toString());
    }
}
