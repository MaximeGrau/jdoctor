package dev.jdoctor.analyze;

import dev.jdoctor.core.Finding;
import java.util.List;

/**
 * A deterministic check. A rule only reports what the data proves, with the numbers as evidence.
 * It returns no finding when the data it needs is missing.
 */
public interface Rule {

    List<Finding> evaluate(Diagnosis diagnosis);
}
