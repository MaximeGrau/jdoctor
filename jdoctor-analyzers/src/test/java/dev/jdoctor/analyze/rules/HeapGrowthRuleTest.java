package dev.jdoctor.analyze.rules;

import static org.assertj.core.api.Assertions.assertThat;

import dev.jdoctor.analyze.Fixtures;
import dev.jdoctor.analyze.parse.JfrSummary;
import dev.jdoctor.core.Finding;
import dev.jdoctor.core.Severity;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;
import org.junit.jupiter.api.Test;

class HeapGrowthRuleTest {

    private static final Instant T0 = Instant.parse("2026-10-28T10:00:00Z");
    private static final long MIB = 1L << 20;

    private final HeapGrowthRule rule = new HeapGrowthRule();

    @Test
    void steadyGrowthIsALeak() {
        // 60 s recording, one collection every 2 s, floor rising 1 MiB per second = 60 MiB/min.
        JfrSummary jfr = recording(60, t -> 100 * MIB + t * MIB, OptionalLong.of(4096 * MIB));

        List<Finding> findings = rule.evaluate(Fixtures.withJfr(jfr));

        assertThat(findings).singleElement().satisfies(f -> {
            assertThat(f.id()).isEqualTo("JD-MEM-001");
            assertThat(f.severity()).isEqualTo(Severity.WARNING);
            assertThat(f.title()).contains("+60 MiB/min");
        });
    }

    @Test
    void growthThatFillsTheHeapWithinAnHourIsCritical() {
        JfrSummary jfr = recording(60, t -> 100 * MIB + t * MIB, OptionalLong.of(512 * MIB));

        assertThat(rule.evaluate(Fixtures.withJfr(jfr))).singleElement()
                .extracting(Finding::severity).isEqualTo(Severity.CRITICAL);
    }

    @Test
    void sawToothWithStableFloorIsNotALeak() {
        // Young garbage piles up between collections, but the floor stays at 100 MiB.
        JfrSummary jfr = recording(60, t -> 100 * MIB + (t % 10) * 8 * MIB, OptionalLong.of(1024 * MIB));

        assertThat(rule.evaluate(Fixtures.withJfr(jfr))).isEmpty();
    }

    @Test
    void tooShortRecordingIsIgnored() {
        JfrSummary jfr = recording(10, t -> 100 * MIB + t * 10 * MIB, OptionalLong.empty());

        assertThat(rule.evaluate(Fixtures.withJfr(jfr))).isEmpty();
    }

    interface Used {
        long at(long second);
    }

    static JfrSummary recording(long seconds, Used used, OptionalLong maxHeap) {
        List<JfrSummary.HeapSample> samples = new ArrayList<>();
        for (long t = 1; t < seconds; t += 2) {
            samples.add(new JfrSummary.HeapSample(T0.plusSeconds(t), used.at(t)));
        }
        return new JfrSummary(T0, T0.plusSeconds(seconds), List.of(), samples, maxHeap);
    }
}
