package dev.jdoctor.llm;

import java.util.List;

/**
 * Extra facts given to the LLM next to the finding. Everything here goes through the
 * {@link Redactor} first.
 *
 * @param jvmSummary short facts about the JVM: version, GC, heap size
 * @param otherFindings ids and titles of the other findings, so the LLM can connect them
 */
public record ExplainContext(List<String> jvmSummary, List<String> otherFindings) {

    public ExplainContext {
        jvmSummary = List.copyOf(jvmSummary);
        otherFindings = List.copyOf(otherFindings);
    }
}
