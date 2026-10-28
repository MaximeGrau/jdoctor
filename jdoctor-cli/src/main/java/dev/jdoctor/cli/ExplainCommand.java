package dev.jdoctor.cli;

import dev.jdoctor.analyze.Analyzer;
import dev.jdoctor.collect.Target;
import dev.jdoctor.core.Finding;
import dev.jdoctor.core.Runbooks;
import dev.jdoctor.llm.ExplainContext;
import dev.jdoctor.llm.Explainer;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

@Command(name = "explain", description = {
    "Explains one finding: its cause and how to fix it.",
    "Without an LLM (--llm none), prints the finding's runbook."
})
final class ExplainCommand implements Callable<Integer> {

    @Spec
    CommandSpec spec;

    @Parameters(index = "0", paramLabel = "ID", description = "Finding id, e.g. JD-MEM-001.")
    String id;

    @Parameters(index = "1", paramLabel = "BUNDLE", description = "Bundle directory or .jfr file the finding comes from.")
    String target;

    @Mixin
    LlmOptions llm;

    @Override
    public Integer call() {
        Target parsed = Target.parse(target);
        if (parsed instanceof Target.LocalPid) {
            throw new IllegalArgumentException("explain works on a bundle; run jdoctor analyze or collect first.");
        }
        Targets.Loaded loaded = Targets.load(parsed, null, null, spec.commandLine().getErr());
        List<Finding> findings = Analyzer.withDefaultRules().analyze(loaded.diagnosis());
        Finding finding = findings.stream()
                .filter(f -> f.id().equalsIgnoreCase(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No finding " + id + " in " + target
                        + ". Found: " + findings.stream().map(Finding::id).toList()));

        Optional<Explainer> explainer = llm.explainer();
        String text;
        if (explainer.isPresent()) {
            List<String> others = findings.stream().filter(f -> f != finding).map(f -> f.id() + ": " + f.title()).toList();
            text = explainer.get().explain(finding, new ExplainContext(JvmFacts.of(loaded.diagnosis()), others));
        } else {
            text = Runbooks.forFinding(finding.id()).orElse("No runbook for " + finding.id() + ".");
        }
        spec.commandLine().getOut().println(text);
        return 0;
    }
}
