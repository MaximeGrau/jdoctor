package dev.jdoctor.analyze.rules;

import static org.assertj.core.api.Assertions.assertThat;

import dev.jdoctor.analyze.Diagnosis;
import dev.jdoctor.analyze.Fixtures;
import dev.jdoctor.core.Finding;
import dev.jdoctor.core.Severity;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class StartupFailureRuleTest {

    private final StartupFailureRule rule = new StartupFailureRule();

    @ParameterizedTest
    @CsvSource({
            "duplicate-bean, JD-START-002",
            "two-candidates, JD-START-003",
            "missing-bean,   JD-START-004",
            "cycle,          JD-START-005",
            "port-in-use,    JD-START-006",
            "init-crash,     JD-START-001",
    })
    void classifiesEachStartupFailure(String name, String id) {
        List<Finding> findings = rule.evaluate(Fixtures.withLog("logs/startup-" + name + ".log"));

        assertThat(findings).singleElement().satisfies(f -> {
            assertThat(f.id()).isEqualTo(id);
            assertThat(f.severity()).isEqualTo(Severity.CRITICAL);
            assertThat(f.evidence()).isNotEmpty();
        });
    }

    @Test
    void springBootActionIsPartOfTheRecommendation() {
        Finding f = rule.evaluate(Fixtures.withLog("logs/startup-duplicate-bean.log")).get(0);

        assertThat(f.evidence()).anyMatch(e -> e.contains("could not be registered"));
        assertThat(f.recommendation()).contains("Spring Boot suggests:");
    }

    @Test
    void springSentencesStaySeparate() {
        assertThat(StartupFailureRule.oneLine("Consider using @Qualifier\n\nEnsure the flag is set.\n"))
                .isEqualTo("Consider using @Qualifier. Ensure the flag is set.");
    }

    @Test
    void unanalyzedCrashPointsAtTheApplicationFrame() {
        Finding f = rule.evaluate(Fixtures.withLog("logs/startup-init-crash.log")).get(0);

        assertThat(f.title()).isEqualTo("Application failed to start: NullPointerException in CrashOnStartup.loadDefaults");
        assertThat(f.evidence()).anyMatch(e -> e.startsWith("Thrown at com.example.orders.failures.CrashOnStartup"));
    }

    @Test
    void runtimeErrorsAreNotStartupFailures() {
        assertThat(rule.evaluate(Fixtures.withLog("logs/runtime-errors.log"))).isEmpty();
        assertThat(rule.evaluate(Diagnosis.empty())).isEmpty();
    }
}
