package dev.jdoctor.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * A directory holding everything collected from one JVM, plus a {@code manifest.json}.
 *
 * <p>The bundle separates collection from analysis: collect in production, analyze on a laptop.
 */
public final class Bundle {

    public static final String MANIFEST = "manifest.json";
    public static final String RECORDING = "recording.jfr";
    public static final String VM_FLAGS = "vm-flags.txt";
    public static final String VM_INFO = "vm-info.txt";
    public static final String HEAP_HISTOGRAM = "heap-histogram.txt";
    public static final String THREAD_DUMP_PREFIX = "thread-dump-";

    private static final ObjectMapper JSON = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(SerializationFeature.INDENT_OUTPUT);

    private final Path dir;
    private final Manifest manifest;

    private Bundle(Path dir, Manifest manifest) {
        this.dir = dir;
        this.manifest = manifest;
    }

    public static Bundle open(Path dir) {
        Path manifestFile = dir.resolve(MANIFEST);
        if (!Files.isRegularFile(manifestFile)) {
            throw new IllegalArgumentException("Not a jdoctor bundle (no " + MANIFEST + "): " + dir);
        }
        try {
            Manifest manifest = JSON.readValue(manifestFile.toFile(), Manifest.class);
            if (manifest.formatVersion() > Manifest.CURRENT_FORMAT_VERSION) {
                throw new IllegalArgumentException("Bundle format " + manifest.formatVersion()
                        + " is newer than this jdoctor supports (" + Manifest.CURRENT_FORMAT_VERSION + ")");
            }
            return new Bundle(dir, manifest);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read " + manifestFile, e);
        }
    }

    /** Writes the manifest into {@code dir} and returns the finished bundle. */
    public static Bundle seal(Path dir, Manifest manifest) {
        try {
            JSON.writeValue(dir.resolve(MANIFEST).toFile(), manifest);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot write manifest in " + dir, e);
        }
        return new Bundle(dir, manifest);
    }

    public Path dir() {
        return dir;
    }

    public Manifest manifest() {
        return manifest;
    }

    public Optional<Path> file(String name) {
        Path path = dir.resolve(name);
        return Files.isRegularFile(path) ? Optional.of(path) : Optional.empty();
    }

    public Optional<String> text(String name) {
        return file(name).map(Bundle::readString);
    }

    /** Thread dumps in the order they were taken. */
    public List<String> threadDumps() {
        try (Stream<Path> files = Files.list(dir)) {
            return files.filter(p -> p.getFileName().toString().startsWith(THREAD_DUMP_PREFIX))
                    .sorted()
                    .map(Bundle::readString)
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot list " + dir, e);
        }
    }

    private static String readString(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read " + path, e);
        }
    }
}
