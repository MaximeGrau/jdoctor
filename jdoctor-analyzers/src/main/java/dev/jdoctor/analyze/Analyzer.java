package dev.jdoctor.analyze;

import dev.jdoctor.analyze.rules.ContainerSizingRule;
import dev.jdoctor.analyze.rules.DeadlockRule;
import dev.jdoctor.analyze.rules.ExceptionHotspotRule;
import dev.jdoctor.analyze.rules.GcOverheadRule;
import dev.jdoctor.analyze.rules.HeapGrowthRule;
import dev.jdoctor.analyze.rules.HikariPoolExhaustionRule;
import dev.jdoctor.analyze.rules.StartupFailureRule;
import dev.jdoctor.core.Finding;
import java.util.Comparator;
import java.util.List;

/** Runs every rule and returns the findings, most severe first. */
public final class Analyzer {

    private final List<Rule> rules;

    public Analyzer(List<Rule> rules) {
        this.rules = List.copyOf(rules);
    }

    public static Analyzer withDefaultRules() {
        return new Analyzer(List.of(
                new HeapGrowthRule(),
                new GcOverheadRule(),
                new DeadlockRule(),
                new HikariPoolExhaustionRule(),
                new ContainerSizingRule(),
                new StartupFailureRule(),
                new ExceptionHotspotRule()));
    }

    public List<Finding> analyze(Diagnosis diagnosis) {
        return rules.stream()
                .flatMap(rule -> rule.evaluate(diagnosis).stream())
                .sorted(Comparator.comparing(Finding::severity).reversed().thenComparing(Finding::id))
                .toList();
    }
}
