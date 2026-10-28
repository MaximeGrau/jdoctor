package dev.jdoctor.llm;

import dev.jdoctor.core.Finding;

/** Turns a proven finding into an explanation and a fix. It never detects anything itself. */
public interface Explainer {

    String explain(Finding finding, ExplainContext context);
}
