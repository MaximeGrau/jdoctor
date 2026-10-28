package dev.jdoctor.core;

import java.time.Instant;
import java.util.List;

/**
 * Describes a bundle: where the data came from and which files it holds.
 *
 * @param formatVersion version of the bundle layout, bumped on breaking changes
 * @param target        the target as the user wrote it, for example {@code pid:4242}
 * @param jdoctorVersion version of jdoctor that collected the bundle
 * @param collectedAt   when the collection started
 * @param durationSeconds length of the JFR recording
 * @param files         names of the files in the bundle directory
 * @param warnings      steps that failed without stopping the collection
 */
public record Manifest(
        int formatVersion,
        String target,
        String jdoctorVersion,
        Instant collectedAt,
        long durationSeconds,
        List<String> files,
        List<String> warnings) {

    public static final int CURRENT_FORMAT_VERSION = 1;

    public Manifest {
        files = List.copyOf(files);
        warnings = List.copyOf(warnings);
    }
}
