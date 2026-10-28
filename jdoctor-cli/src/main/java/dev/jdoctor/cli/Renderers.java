package dev.jdoctor.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.jdoctor.cli.render.Format;
import dev.jdoctor.core.Finding;
import dev.jdoctor.core.Report;
import dev.jdoctor.core.Severity;

/** Prints a report as colored terminal text, Markdown or JSON. */
final class Renderers {

    private static final ObjectMapper JSON = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(SerializationFeature.INDENT_OUTPUT);

    private Renderers() {
    }

    static String render(Report report, Format format, boolean color) {
        return switch (format) {
            case TEXT -> text(report, color);
            case MD -> markdown(report);
            case JSON -> json(report);
        };
    }

    static String text(Report report, boolean color) {
        StringBuilder sb = new StringBuilder();
        sb.append(bold(report.producer() + " · " + report.subject(), color)).append('\n');
        report.facts().forEach(f -> sb.append("  ").append(f).append('\n'));
        sb.append('\n');
        if (report.findings().isEmpty()) {
            sb.append(paint("No problem found by the current rules.", "32", color)).append('\n');
            return sb.toString();
        }
        for (Finding f : report.findings()) {
            sb.append(severity(f.severity(), color)).append("  ").append(bold(f.id(), color)).append("  ")
                    .append(f.title()).append('\n');
            f.evidence().forEach(e -> sb.append("                     ").append(e).append('\n'));
            sb.append("           ").append(paint("→ " + f.recommendation(), "2", color)).append('\n');
            String explanation = report.explanations().get(f.id());
            if (explanation != null) {
                sb.append('\n');
                explanation.lines().forEach(l -> sb.append("           ").append(l).append('\n'));
            }
            sb.append('\n');
        }
        sb.append(summary(report));
        return sb.toString();
    }

    static String markdown(Report report) {
        StringBuilder sb = new StringBuilder();
        sb.append("# jdoctor report: ").append(report.subject()).append("\n\n");
        sb.append(report.producer()).append(", ").append(report.createdAt()).append("\n\n");
        report.facts().forEach(f -> sb.append("- ").append(f).append('\n'));
        sb.append('\n');
        if (report.findings().isEmpty()) {
            sb.append("No problem found by the current rules.\n");
            return sb.toString();
        }
        sb.append("| Severity | Id | Finding |\n| --- | --- | --- |\n");
        report.findings().forEach(f -> sb.append("| ").append(f.severity()).append(" | ").append(f.id())
                .append(" | ").append(f.title().replace("|", "\\|")).append(" |\n"));
        for (Finding f : report.findings()) {
            sb.append("\n## ").append(f.id()).append(": ").append(f.title()).append("\n\n");
            f.evidence().forEach(e -> sb.append("- ").append(e).append('\n'));
            sb.append("\n**Recommendation.** ").append(f.recommendation()).append('\n');
            String explanation = report.explanations().get(f.id());
            if (explanation != null) {
                sb.append("\n### Explanation\n\n").append(explanation).append('\n');
            }
        }
        return sb.toString();
    }

    static String json(Report report) {
        try {
            return JSON.writeValueAsString(report) + "\n";
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize report", e);
        }
    }

    private static String summary(Report report) {
        long critical = report.findings().stream().filter(f -> f.severity() == Severity.CRITICAL).count();
        long warning = report.findings().stream().filter(f -> f.severity() == Severity.WARNING).count();
        return critical + " critical, " + warning + " warning. Explain one with: jdoctor explain <ID> <bundle>\n";
    }

    private static String severity(Severity severity, boolean color) {
        String label = String.format("%-8s", severity.name());
        return switch (severity) {
            case CRITICAL -> paint(label, "1;31", color);
            case WARNING -> paint(label, "1;33", color);
            case INFO -> paint(label, "36", color);
        };
    }

    private static String bold(String s, boolean color) {
        return paint(s, "1", color);
    }

    private static String paint(String s, String code, boolean color) {
        return color ? "\u001B[" + code + "m" + s + "\u001B[0m" : s;
    }
}
