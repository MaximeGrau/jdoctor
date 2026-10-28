package dev.jdoctor.analyze.parse;

import java.util.List;

/**
 * One thread dump.
 *
 * @param threads  every thread with its state and stack
 * @param deadlock the JVM's deadlock report, empty when it found none
 */
public record ThreadDump(List<ThreadInfo> threads, String deadlock) {

    public ThreadDump {
        threads = List.copyOf(threads);
    }

    public boolean hasDeadlock() {
        return !deadlock.isEmpty();
    }

    /**
     * @param name   thread name
     * @param state  {@code java.lang.Thread.State} value, or {@code UNKNOWN} for VM threads
     * @param frames stack frames, innermost first, without the leading {@code at }
     */
    public record ThreadInfo(String name, String state, List<String> frames) {

        public ThreadInfo {
            frames = List.copyOf(frames);
        }

        public boolean hasFrameStartingWith(String prefix) {
            return frames.stream().anyMatch(f -> f.startsWith(prefix));
        }
    }
}
