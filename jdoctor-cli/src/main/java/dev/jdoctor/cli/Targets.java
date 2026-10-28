package dev.jdoctor.cli;

import dev.jdoctor.analyze.Diagnosis;
import dev.jdoctor.analyze.DiagnosisLoader;
import dev.jdoctor.analyze.parse.JfrParser;
import dev.jdoctor.analyze.parse.LogParser;
import dev.jdoctor.collect.CollectOptions;
import dev.jdoctor.collect.Jcmd;
import dev.jdoctor.collect.LocalCollector;
import dev.jdoctor.collect.Target;
import dev.jdoctor.core.Bundle;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/** Turns a target into a diagnosis, collecting first when the target is a live JVM. */
final class Targets {

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private Targets() {
    }

    record Loaded(String description, Diagnosis diagnosis, Optional<Bundle> bundle) {
    }

    /**
     * @param log an application log to read with the target, or null; it is copied into a bundle
     *            collected now, and only read for a bundle that already exists
     */
    static Loaded load(Target target, CollectOptions options, Path outDir, Path log, PrintWriter progress) {
        Loaded loaded = switch (target) {
            case Target.LocalPid pid -> {
                Bundle bundle = collect(pid, options, outDir, log, progress);
                yield new Loaded(pid.spec(), DiagnosisLoader.load(bundle), Optional.of(bundle));
            }
            case Target.BundleDir dir -> {
                Bundle bundle = Bundle.open(dir.dir());
                yield new Loaded(bundle.manifest().target() + " (bundle " + dir.dir() + ")",
                        DiagnosisLoader.load(bundle), Optional.of(bundle));
            }
            case Target.JfrFile file -> new Loaded(file.spec(),
                    Diagnosis.empty().withJfr(JfrParser.parse(file.file())), Optional.empty());
            case Target.LogFile file -> new Loaded(file.spec(),
                    Diagnosis.empty().withLog(LogParser.parse(file.file())), Optional.empty());
        };
        if (log != null && !(target instanceof Target.LocalPid)) {
            return new Loaded(loaded.description(), loaded.diagnosis().withLog(LogParser.parse(log)), loaded.bundle());
        }
        return loaded;
    }

    static Bundle collect(Target.LocalPid pid, CollectOptions options, Path outDir, Path log, PrintWriter progress) {
        if (log != null && !Files.isRegularFile(log)) {
            throw new IllegalArgumentException("--log " + log + " is not a file.");
        }
        Path dir = outDir != null ? outDir
                : Path.of("jdoctor-bundle-" + pid.pid() + "-" + LocalDateTime.now().format(STAMP));
        LocalCollector collector = new LocalCollector(new Jcmd(), message -> {
            progress.println("  " + message);
            progress.flush();
        });
        progress.println("Collecting from " + pid.spec() + " into " + dir);
        Bundle bundle = collector.collect(pid.pid(), options, dir);
        if (log != null) {
            bundle = bundle.attach(log, Bundle.APP_LOG);
            progress.println("  Copied " + log + " into the bundle");
        }
        bundle.manifest().warnings().forEach(w -> progress.println("  warning: " + w));
        progress.flush();
        return bundle;
    }
}
