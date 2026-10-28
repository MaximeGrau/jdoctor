package dev.jdoctor.collect;

import java.time.Duration;

/**
 * @param duration      length of the JFR recording
 * @param threadDumps   number of thread dumps, spread over the recording
 * @param heapHistogram whether to take a class histogram; it triggers a full GC in the target
 */
public record CollectOptions(Duration duration, int threadDumps, boolean heapHistogram) {

    public CollectOptions {
        if (duration.isNegative() || duration.isZero()) {
            throw new IllegalArgumentException("duration must be positive");
        }
        if (threadDumps < 0) {
            throw new IllegalArgumentException("threadDumps must be >= 0");
        }
    }

    public static CollectOptions defaults() {
        return new CollectOptions(Duration.ofSeconds(60), 3, true);
    }
}
