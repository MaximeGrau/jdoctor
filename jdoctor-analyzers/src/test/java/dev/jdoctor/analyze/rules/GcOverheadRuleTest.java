package dev.jdoctor.analyze.rules;

import static org.assertj.core.api.Assertions.assertThat;

import dev.jdoctor.analyze.Fixtures;
import dev.jdoctor.analyze.parse.JfrSummary;
import dev.jdoctor.core.Finding;
import dev.jdoctor.core.Severity;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;
import org.junit.jupiter.api.Test;

class GcOverheadRuleTest {

    private static final Instant T0 = Instant.parse("2026-10-28T10:00:00Z");

    private final GcOverheadRule rule = new GcOverheadRule();

    @Test
    void twentyPercentPausedIsCritical() {
        // 60 s, 60 collections of 200 ms = 12 s paused.
        List<Finding> findings = rule.evaluate(Fixtures.withJfr(recording(60, 60, 200, "G1 Evacuation Pause")));

        assertThat(findings).singleElement().satisfies(f -> {
            assertThat(f.severity()).isEqualTo(Severity.CRITICAL);
            assertThat(f.title()).contains("20.0 %");
            assertThat(f.evidence()).anyMatch(e -> e.contains("G1 Evacuation Pause"));
        });
    }

    @Test
    void explicitGcCallsGetASpecificRecommendation() {
        List<Finding> findings = rule.evaluate(Fixtures.withJfr(recording(60, 30, 150, "System.gc()")));

        assertThat(findings).singleElement().satisfies(f -> {
            assertThat(f.severity()).isEqualTo(Severity.WARNING);
            assertThat(f.recommendation()).contains("DisableExplicitGC");
        });
    }

    @Test
    void lowOverheadIsFine() {
        assertThat(rule.evaluate(Fixtures.withJfr(recording(60, 20, 10, "G1 Evacuation Pause")))).isEmpty();
    }

    @Test
    void oneLongPauseIsAWarning() {
        JfrSummary jfr = new JfrSummary(T0, T0.plusSeconds(60),
                List.of(new JfrSummary.GcEvent(T0.plusSeconds(5), "G1Full", "Allocation Failure",
                        Duration.ofMillis(800), Duration.ofMillis(800))),
                List.of(), OptionalLong.empty());

        assertThat(rule.evaluate(Fixtures.withJfr(jfr))).singleElement()
                .extracting(Finding::severity).isEqualTo(Severity.WARNING);
    }

    static JfrSummary recording(long seconds, int collections, long pauseMillis, String cause) {
        List<JfrSummary.GcEvent> events = new ArrayList<>();
        for (int i = 0; i < collections; i++) {
            Duration pause = Duration.ofMillis(pauseMillis);
            events.add(new JfrSummary.GcEvent(T0.plusMillis(i * 900L), "G1New", cause, pause, pause));
        }
        return new JfrSummary(T0, T0.plusSeconds(seconds), events, List.of(), OptionalLong.empty());
    }
}
