package dev.jdoctor.analyze.parse;

import java.util.List;
import java.util.Optional;

/**
 * What the rules need from an application log.
 *
 * @param startupFailure the last Spring Boot "APPLICATION FAILED TO START" report, if any
 * @param exceptions     every stack trace found in the log, in order
 */
public record LogSummary(Optional<StartupFailure> startupFailure, List<LoggedException> exceptions) {

    public LogSummary {
        exceptions = List.copyOf(exceptions);
    }

    /** Spring Boot's own analysis of a failed startup, printed by its FailureAnalyzers. */
    public record StartupFailure(String description, String action) {
    }

    /**
     * One stack trace with its "Caused by" chain.
     *
     * @param chain         outermost exception first, root cause last
     * @param duringStartup true when Spring Boot logged it as "Application run failed"
     */
    public record LoggedException(List<Cause> chain, boolean duringStartup) {

        public LoggedException {
            chain = List.copyOf(chain);
        }

        public Cause rootCause() {
            return chain.get(chain.size() - 1);
        }
    }

    /**
     * @param type    fully qualified exception class
     * @param message exception message, empty when there is none
     * @param frames  normalized frames, such as {@code com.example.Foo.bar(Foo.java:12)}
     */
    public record Cause(String type, String message, List<String> frames) {

        public Cause {
            frames = List.copyOf(frames);
        }

        public String simpleType() {
            return type.substring(type.lastIndexOf('.') + 1);
        }
    }
}
