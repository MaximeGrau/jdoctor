package dev.jdoctor.cli;

import dev.jdoctor.analyze.Diagnosis;
import dev.jdoctor.analyze.DiagnosisLoader;
import dev.jdoctor.analyze.parse.JfrParser;
import dev.jdoctor.collect.CollectOptions;
import dev.jdoctor.collect.Jcmd;
import dev.jdoctor.collect.LocalCollector;
import dev.jdoctor.collect.Target;
import dev.jdoctor.core.Bundle;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/** Turns a target into a diagnosis, collecting first when the target is a live JVM. */
final class Targets {

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private Targets() {
    }

    record Loaded(String description, Diagnosis diagnosis, Optional<Bundle> bundle) {
    }

    static Loaded load(Target target, CollectOptions options, Path outDir, PrintWriter progress) {
        return switch (target) {
            case Target.LocalPid pid -> {
                Bundle bundle = collect(pid, options, outDir, progress);
                yield new Loaded(pid.spec(), DiagnosisLoader.load(bundle), Optional.of(bundle));
            }
            case Target.BundleDir dir -> {
                Bundle bundle = Bundle.open(dir.dir());
                yield new Loaded(bundle.manifest().target() + " (bundle " + dir.dir() + ")",
                        DiagnosisLoader.load(bundle), Optional.of(bundle));
            }
            case Target.JfrFile file -> new Loaded(file.spec(),
                    new Diagnosis(Optional.of(JfrParser.parse(file.file())), List.of(), Optional.empty(),
                            Optional.empty(), List.of()),
                    Optional.empty());
        };
    }

    static Bundle collect(Target.LocalPid pid, CollectOptions options, Path outDir, PrintWriter progress) {
        Path dir = outDir != null ? outDir
                : Path.of("jdoctor-bundle-" + pid.pid() + "-" + LocalDateTime.now().format(STAMP));
        LocalCollector collector = new LocalCollector(new Jcmd(), message -> {
            progress.println("  " + message);
            progress.flush();
        });
        progress.println("Collecting from " + pid.spec() + " into " + dir);
        Bundle bundle = collector.collect(pid.pid(), options, dir);
        bundle.manifest().warnings().forEach(w -> progress.println("  warning: " + w));
        progress.flush();
        return bundle;
    }
}
