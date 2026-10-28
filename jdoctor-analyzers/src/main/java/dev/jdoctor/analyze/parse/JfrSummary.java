package dev.jdoctor.analyze.parse;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.OptionalLong;

/** What the rules need from a JFR recording, already extracted. */
public record JfrSummary(
        Instant start,
        Instant end,
        List<GcEvent> collections,
        List<HeapSample> heapAfterGc,
        OptionalLong maxHeapBytes) {

    public JfrSummary {
        collections = List.copyOf(collections);
        heapAfterGc = List.copyOf(heapAfterGc);
    }

    public Duration duration() {
        return Duration.between(start, end);
    }

    /** One garbage collection. Pauses are stop-the-world time, not the concurrent phases. */
    public record GcEvent(Instant start, String name, String cause, Duration sumOfPauses, Duration longestPause) {
    }

    /** Heap in use right after a collection: the live set plus what the GC did not reclaim. */
    public record HeapSample(Instant time, long usedBytes) {
    }
}
