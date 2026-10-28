package dev.jdoctor.analyze.rules;

import static org.assertj.core.api.Assertions.assertThat;

import dev.jdoctor.analyze.Fixtures;
import dev.jdoctor.analyze.parse.ContainerInfo;
import dev.jdoctor.core.Finding;
import dev.jdoctor.core.Severity;
import java.util.OptionalInt;
import java.util.OptionalLong;
import org.junit.jupiter.api.Test;

class ContainerSizingRuleTest {

    private static final long MIB = 1L << 20;

    private final ContainerSizingRule rule = new ContainerSizingRule();

    @Test
    void heapAt94PercentOfTheLimitIsCritical() {
        // -Xmx3g in a 3.2 GiB pod.
        var findings = rule.evaluate(Fixtures.withSizing("-XX:MaxHeapSize=" + 3072 * MIB, limit(3277 * MIB)));

        assertThat(findings).singleElement().satisfies(f -> {
            assertThat(f.id()).isEqualTo("JD-SIZE-001");
            assertThat(f.severity()).isEqualTo(Severity.CRITICAL);
            assertThat(f.title()).contains("93.7 %");
        });
    }

    @Test
    void heapAt85PercentIsAWarning() {
        assertThat(rule.evaluate(Fixtures.withSizing("-XX:MaxHeapSize=" + 850 * MIB, limit(1000 * MIB))))
                .singleElement().extracting(Finding::severity).isEqualTo(Severity.WARNING);
    }

    @Test
    void seventyFivePercentIsFine() {
        assertThat(rule.evaluate(Fixtures.withSizing("-XX:MaxHeapSize=" + 750 * MIB, limit(1000 * MIB)))).isEmpty();
    }

    @Test
    void noLimitNoFinding() {
        ContainerInfo unlimited = new ContainerInfo("cgroupv2", OptionalLong.empty(), OptionalInt.empty());
        assertThat(rule.evaluate(Fixtures.withSizing("-XX:MaxHeapSize=" + 750 * MIB, unlimited))).isEmpty();
    }

    private static ContainerInfo limit(long bytes) {
        return new ContainerInfo("cgroupv2", OptionalLong.of(bytes), OptionalInt.of(2));
    }
}
