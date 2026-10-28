package dev.jdoctor.collect;

import dev.jdoctor.core.Bundle;
import dev.jdoctor.core.Manifest;
import dev.jdoctor.core.Version;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Collects a bundle from a JVM on this machine with {@code jcmd}.
 *
 * <p>Sequence: flags and VM info, start a JFR recording, take thread dumps spread over the
 * recording, dump and stop the recording, then the optional heap histogram. A failed optional
 * step becomes a warning in the manifest instead of aborting the collection.
 */
public final class LocalCollector {

    private static final String RECORDING_NAME = "jdoctor";

    private final Jcmd jcmd;
    private final Consumer<String> progress;

    public LocalCollector(Jcmd jcmd, Consumer<String> progress) {
        this.jcmd = jcmd;
        this.progress = progress;
    }

    public Bundle collect(long pid, CollectOptions options, Path outDir) {
        Instant start = Instant.now();
        List<String> files = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        createDir(outDir);

        // Fails fast with a clear message if the JVM is not reachable.
        write(outDir, Bundle.VM_FLAGS, jcmd.run(pid, "VM.flags"), files);
        optional(warnings, "VM.info", () -> write(outDir, Bundle.VM_INFO, jcmd.run(pid, "VM.info"), files));

        Path recording = outDir.resolve(Bundle.RECORDING).toAbsolutePath();
        progress.accept("Recording JFR for " + options.duration().toSeconds() + " s");
        // dumponexit keeps the recording if the target dies during collection, which is when it matters most.
        jcmd.run(pid, "JFR.start", "name=" + RECORDING_NAME, "settings=profile", "dumponexit=true",
                "filename=" + recording);
        try {
            takeThreadDumps(pid, options, outDir, files, warnings);
        } finally {
            dumpAndStopRecording(pid, recording, warnings);
        }
        if (Files.isRegularFile(recording)) {
            files.add(Bundle.RECORDING);
        }
        if (ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false) == Boolean.FALSE) {
            warnings.add("The target JVM exited during the collection");
            progress.accept("warning: the target JVM exited during the collection");
        }

        if (options.heapHistogram() && ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false)) {
            progress.accept("Taking heap histogram");
            optional(warnings, "GC.class_histogram",
                    () -> write(outDir, Bundle.HEAP_HISTOGRAM, jcmd.run(pid, "GC.class_histogram"), files));
        }

        Manifest manifest = new Manifest(Manifest.CURRENT_FORMAT_VERSION, "pid:" + pid, Version.current(),
                start, options.duration().toSeconds(), files, warnings);
        return Bundle.seal(outDir, manifest);
    }

    /** Spreads the dumps evenly so that a stuck thread shows up in several of them. */
    private void takeThreadDumps(long pid, CollectOptions options, Path outDir, List<String> files,
            List<String> warnings) {
        int count = options.threadDumps();
        Duration slice = options.duration().dividedBy(count + 1L);
        for (int i = 1; i <= count; i++) {
            sleep(slice);
            progress.accept("Thread dump " + i + "/" + count);
            String name = Bundle.THREAD_DUMP_PREFIX + i + ".txt";
            optional(warnings, "Thread.print #" + i, () -> write(outDir, name, jcmd.run(pid, "Thread.print", "-l"), files));
        }
        sleep(slice);
    }

    private void dumpAndStopRecording(long pid, Path recording, List<String> warnings) {
        optional(warnings, "JFR.dump",
                () -> jcmd.run(pid, "JFR.dump", "name=" + RECORDING_NAME, "filename=" + recording));
        optional(warnings, "JFR.stop", () -> jcmd.run(pid, "JFR.stop", "name=" + RECORDING_NAME));
    }

    private static void optional(List<String> warnings, String step, Runnable action) {
        try {
            action.run();
        } catch (CollectionException e) {
            warnings.add(step + " failed: " + e.getMessage());
        }
    }

    private static void write(Path dir, String name, String content, List<String> files) {
        try {
            Files.writeString(dir.resolve(name), content);
            files.add(name);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot write " + name, e);
        }
    }

    private static void createDir(Path dir) {
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot create " + dir, e);
        }
    }

    private static void sleep(Duration duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CollectionException("Interrupted during collection", e);
        }
    }
}
