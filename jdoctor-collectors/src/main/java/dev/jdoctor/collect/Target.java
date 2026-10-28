package dev.jdoctor.collect;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Where the data comes from: a live JVM, or files collected earlier.
 *
 * <p>Syntax: {@code pid:4242}, {@code 4242}, or a path to a bundle directory, a {@code .jfr} file,
 * or an application log ({@code .log}, {@code .txt}, {@code .out}).
 * {@code docker:} and {@code k8s:} targets are planned for v0.1.
 */
public sealed interface Target {

    String spec();

    record LocalPid(long pid) implements Target {
        @Override
        public String spec() {
            return "pid:" + pid;
        }
    }

    record BundleDir(Path dir) implements Target {
        @Override
        public String spec() {
            return dir.toString();
        }
    }

    record JfrFile(Path file) implements Target {
        @Override
        public String spec() {
            return file.toString();
        }
    }

    record LogFile(Path file) implements Target {
        @Override
        public String spec() {
            return file.toString();
        }
    }

    static Target parse(String spec) {
        if (spec.startsWith("pid:")) {
            return new LocalPid(parsePid(spec.substring(4), spec));
        }
        if (spec.chars().allMatch(Character::isDigit) && !spec.isEmpty()) {
            return new LocalPid(parsePid(spec, spec));
        }
        if (spec.startsWith("docker:") || spec.startsWith("k8s:")) {
            throw new IllegalArgumentException("Target '" + spec
                    + "' is not supported yet: Docker and Kubernetes targets are planned for v0.1."
                    + " Meanwhile, run jdoctor collect inside the container and analyze the bundle.");
        }
        Path path = Path.of(spec);
        if (Files.isDirectory(path)) {
            return new BundleDir(path);
        }
        if (Files.isRegularFile(path) && spec.endsWith(".jfr")) {
            return new JfrFile(path);
        }
        if (Files.isRegularFile(path) && isLog(spec)) {
            return new LogFile(path);
        }
        throw new IllegalArgumentException("Unknown target '" + spec
                + "'. Use pid:<pid>, a bundle directory, a .jfr file, or a .log file.");
    }

    static boolean isLog(String name) {
        return name.endsWith(".log") || name.endsWith(".txt") || name.endsWith(".out");
    }

    private static long parsePid(String value, String spec) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid pid in target '" + spec + "'", e);
        }
    }
}
