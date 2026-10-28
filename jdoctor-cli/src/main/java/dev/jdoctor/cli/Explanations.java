package dev.jdoctor.cli;

import dev.jdoctor.core.Finding;
import dev.jdoctor.core.Severity;
import dev.jdoctor.llm.ExplainContext;
import dev.jdoctor.llm.Explainer;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Asks the explainer about each finding worth explaining; a failure never hides the findings. */
final class Explanations {

    private Explanations() {
    }

    static Map<String, String> forFindings(Explainer explainer, List<Finding> findings, List<String> jvm,
            PrintWriter progress) {
        Map<String, String> result = new LinkedHashMap<>();
        for (Finding finding : findings) {
            if (!finding.severity().isAtLeast(Severity.WARNING)) {
                continue;
            }
            progress.println("  Asking the LLM about " + finding.id());
            progress.flush();
            List<String> others = findings.stream()
                    .filter(f -> f != finding)
                    .map(f -> f.id() + ": " + f.title())
                    .toList();
            try {
                result.put(finding.id(), explainer.explain(finding, new ExplainContext(jvm, others)));
            } catch (RuntimeException e) {
                progress.println("  warning: no explanation for " + finding.id() + ": " + e.getMessage());
            }
        }
        return result;
    }
}
