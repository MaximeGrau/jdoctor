package dev.jdoctor.analyze;

import dev.jdoctor.analyze.parse.ContainerInfo;
import dev.jdoctor.analyze.parse.HeapHistogram;
import dev.jdoctor.analyze.parse.JfrSummary;
import dev.jdoctor.analyze.parse.LogSummary;
import dev.jdoctor.analyze.parse.ThreadDump;
import dev.jdoctor.analyze.parse.VmFlags;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * Everything parsed from a bundle or a log. Each part is optional: each collection step can fail,
 * and a log alone has no JVM data.
 */
public record Diagnosis(
        Optional<JfrSummary> jfr,
        List<ThreadDump> threadDumps,
        Optional<VmFlags> flags,
        Optional<ContainerInfo> container,
        List<HeapHistogram.Entry> histogram,
        Optional<LogSummary> log) {

    public Diagnosis {
        threadDumps = List.copyOf(threadDumps);
        histogram = List.copyOf(histogram);
    }

    public static Diagnosis empty() {
        return new Diagnosis(Optional.empty(), List.of(), Optional.empty(), Optional.empty(), List.of(),
                Optional.empty());
    }

    public Diagnosis withJfr(JfrSummary summary) {
        return new Diagnosis(Optional.of(summary), threadDumps, flags, container, histogram, log);
    }

    public Diagnosis withLog(LogSummary summary) {
        return new Diagnosis(jfr, threadDumps, flags, container, histogram, Optional.of(summary));
    }

    /** Max heap from the flags, falling back to the JFR heap configuration. */
    public OptionalLong maxHeapBytes() {
        OptionalLong fromFlags = flags.map(VmFlags::maxHeapBytes).orElse(OptionalLong.empty());
        if (fromFlags.isPresent()) {
            return fromFlags;
        }
        return jfr.map(JfrSummary::maxHeapBytes).orElse(OptionalLong.empty());
    }
}
