package dev.jdoctor.core;

import java.util.List;
import java.util.Objects;

/**
 * One problem detected by a rule, with the evidence that proves it.
 *
 * @param id             stable identifier, for example {@code JD-MEM-001}; each id has a runbook
 * @param severity       how urgent the problem is
 * @param title          one-line summary with the key number
 * @param evidence       measured facts that support the finding, one per line
 * @param recommendation what to do next, without the LLM
 */
public record Finding(String id, Severity severity, String title, List<String> evidence, String recommendation) {

    public Finding {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(severity, "severity");
        Objects.requireNonNull(title, "title");
        evidence = List.copyOf(evidence);
        Objects.requireNonNull(recommendation, "recommendation");
    }
}
