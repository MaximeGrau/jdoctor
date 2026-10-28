package dev.jdoctor.analyze.parse;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.OptionalLong;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;

/** Reads a {@code .jfr} file with the JDK's own parser. */
public final class JfrParser {

    private JfrParser() {
    }

    public static JfrSummary parse(Path file) {
        Instant start = Instant.MAX;
        Instant end = Instant.MIN;
        List<JfrSummary.GcEvent> collections = new ArrayList<>();
        List<JfrSummary.HeapSample> afterGc = new ArrayList<>();
        OptionalLong maxHeap = OptionalLong.empty();

        try (RecordingFile recording = new RecordingFile(file)) {
            while (recording.hasMoreEvents()) {
                RecordedEvent event = recording.readEvent();
                if (event.getStartTime().isBefore(start)) {
                    start = event.getStartTime();
                }
                if (event.getEndTime().isAfter(end)) {
                    end = event.getEndTime();
                }
                switch (event.getEventType().getName()) {
                    case "jdk.GarbageCollection" -> collections.add(new JfrSummary.GcEvent(
                            event.getStartTime(),
                            event.getString("name"),
                            event.getString("cause"),
                            durationOrZero(event, "sumOfPauses"),
                            durationOrZero(event, "longestPause")));
                    case "jdk.GCHeapSummary" -> {
                        if ("After GC".equals(event.getString("when"))) {
                            afterGc.add(new JfrSummary.HeapSample(event.getStartTime(), event.getLong("heapUsed")));
                        }
                    }
                    case "jdk.GCHeapConfiguration" -> maxHeap = OptionalLong.of(event.getLong("maxSize"));
                    default -> {
                        // Other events are not used by the current rules.
                    }
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read JFR file " + file, e);
        }

        if (start.equals(Instant.MAX)) {
            throw new IllegalArgumentException("JFR file has no events: " + file);
        }
        collections.sort(Comparator.comparing(JfrSummary.GcEvent::start));
        afterGc.sort(Comparator.comparing(JfrSummary.HeapSample::time));
        return new JfrSummary(start, end, collections, afterGc, maxHeap);
    }

    private static Duration durationOrZero(RecordedEvent event, String field) {
        Duration value = event.hasField(field) ? event.getDuration(field) : null;
        return value != null ? value : Duration.ZERO;
    }
}
