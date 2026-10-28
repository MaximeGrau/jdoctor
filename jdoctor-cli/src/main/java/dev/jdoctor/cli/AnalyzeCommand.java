package dev.jdoctor.cli;

import dev.jdoctor.analyze.Analyzer;
import dev.jdoctor.cli.render.Format;
import dev.jdoctor.collect.Target;
import dev.jdoctor.core.Finding;
import dev.jdoctor.core.Report;
import dev.jdoctor.core.Severity;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Help;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

@Command(name = "analyze", description = {
    "Diagnoses a JVM and prints the findings.",
    "TARGET is a live JVM (pid:<pid>), a bundle directory, a .jfr file, or an application log."
})
final class AnalyzeCommand implements Callable<Integer> {

    enum FailOn { NONE, WARNING, CRITICAL }

    @Spec
    CommandSpec spec;

    @Parameters(index = "0", paramLabel = "TARGET")
    String target;

    @Option(names = {"-o", "--out"}, description = "Where to keep the bundle collected from a live JVM.")
    Path out;

    @Option(names = "--log", paramLabel = "FILE", description = "Application log to analyze with the JVM data: startup failures and exceptions.")
    Path log;

    @Option(names = {"-f", "--format"}, defaultValue = "text", description = "Output format: ${COMPLETION-CANDIDATES} (default: ${DEFAULT-VALUE}).")
    Format format;

    @Option(names = "--report", description = "Write the report to this file instead of the terminal.")
    Path reportFile;

    @Option(names = "--explain", negatable = false, description = "Ask the LLM to explain each warning and critical finding.")
    boolean explain;

    @Option(names = "--fail-on", defaultValue = "none", description = "Exit with code 2 when a finding reaches this severity: ${COMPLETION-CANDIDATES}.")
    FailOn failOn;

    @Mixin
    CollectionOptions collection;

    @Mixin
    LlmOptions llm;

    @Override
    public Integer call() throws IOException {
        PrintWriter err = spec.commandLine().getErr();
        Targets.Loaded loaded = Targets.load(Target.parse(target), collection.toOptions(), out, log, err);
        List<Finding> findings = Analyzer.withDefaultRules().analyze(loaded.diagnosis());

        List<String> jvm = JvmFacts.of(loaded.diagnosis());
        Map<String, String> explanations = explain && !findings.isEmpty()
                ? llm.explainer().map(e -> Explanations.forFindings(e, llm, findings, jvm, err)).orElse(Map.of())
                : Map.of();
        Report report = Report.create(Report.KIND_JVM, loaded.description(), jvm, findings, explanations);

        if (reportFile != null) {
            Files.writeString(reportFile, Renderers.render(report, format, false));
            err.println("Report written to " + reportFile);
        } else {
            boolean color = format == Format.TEXT && Help.Ansi.AUTO.enabled();
            PrintWriter out = spec.commandLine().getOut();
            out.print(Renderers.render(report, format, color));
            out.flush();
        }
        loaded.bundle().ifPresent(b -> err.println("Bundle: " + b.dir()));
        err.flush();
        return exitCode(findings);
    }

    private int exitCode(List<Finding> findings) {
        if (failOn == FailOn.NONE) {
            return 0;
        }
        Severity threshold = failOn == FailOn.WARNING ? Severity.WARNING : Severity.CRITICAL;
        return findings.stream().anyMatch(f -> f.severity().isAtLeast(threshold)) ? JDoctor.EXIT_FINDINGS : 0;
    }
}
