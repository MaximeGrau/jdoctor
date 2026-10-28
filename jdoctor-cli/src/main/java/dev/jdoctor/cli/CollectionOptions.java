package dev.jdoctor.cli;

import dev.jdoctor.collect.CollectOptions;
import java.time.Duration;
import picocli.CommandLine.Option;

/** Options shared by the commands that collect from a live JVM. */
final class CollectionOptions {

    @Option(names = "--duration", defaultValue = "60s", description = "JFR recording length, e.g. 30s or 2m (default: ${DEFAULT-VALUE}).")
    Duration duration;

    @Option(names = "--thread-dumps", defaultValue = "3", description = "Thread dumps spread over the recording (default: ${DEFAULT-VALUE}).")
    int threadDumps;

    @Option(names = "--no-histogram", negatable = false, description = "Skip the heap histogram, which triggers a full GC in the target.")
    boolean noHistogram;

    CollectOptions toOptions() {
        return new CollectOptions(duration, threadDumps, !noHistogram);
    }
}
