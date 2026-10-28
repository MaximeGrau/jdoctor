package dev.jdoctor.analyze;

import dev.jdoctor.analyze.parse.ContainerInfo;
import dev.jdoctor.analyze.parse.HeapHistogram;
import dev.jdoctor.analyze.parse.JfrSummary;
import dev.jdoctor.analyze.parse.ThreadDump;
import dev.jdoctor.analyze.parse.VmFlags;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

/** Everything parsed from a bundle. Each part is optional because each collection step can fail. */
public record Diagnosis(
        Optional<JfrSummary> jfr,
        List<ThreadDump> threadDumps,
        Optional<VmFlags> flags,
        Optional<ContainerInfo> container,
        List<HeapHistogram.Entry> histogram) {

    public Diagnosis {
        threadDumps = List.copyOf(threadDumps);
        histogram = List.copyOf(histogram);
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
