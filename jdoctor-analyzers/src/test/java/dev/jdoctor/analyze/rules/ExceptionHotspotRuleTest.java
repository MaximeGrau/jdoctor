package dev.jdoctor.analyze.rules;

import static org.assertj.core.api.Assertions.assertThat;

import dev.jdoctor.analyze.Diagnosis;
import dev.jdoctor.analyze.Fixtures;
import dev.jdoctor.analyze.parse.LogParser;
import dev.jdoctor.core.Finding;
import dev.jdoctor.core.Severity;
import java.util.List;
import org.junit.jupiter.api.Test;

class ExceptionHotspotRuleTest {

    private final ExceptionHotspotRule rule = new ExceptionHotspotRule();

    @Test
    void separatesBugsFromApplicationExceptions() {
        List<Finding> findings = rule.evaluate(Fixtures.withLog("logs/runtime-errors.log"));

        assertThat(findings).extracting(Finding::id).containsExactly("JD-EXC-001", "JD-EXC-002");
        assertThat(findings.get(0)).satisfies(f -> {
            assertThat(f.severity()).isEqualTo(Severity.WARNING);
            assertThat(f.title()).isEqualTo("NullPointerException in OrderController.size (5 times)");
            assertThat(f.evidence()).singleElement().asString()
                    .startsWith("5 times NullPointerException at com.example.orders.OrderController.size(")
                    .endsWith("Cannot read the array length because \"order\" is null");
            assertThat(f.recommendation()).contains("null");
        });
        assertThat(findings.get(1)).satisfies(f -> {
            assertThat(f.title()).isEqualTo("Application exception OrderNotFoundException escapes unhandled (3 times)");
            assertThat(f.evidence()).singleElement().asString()
                    .startsWith("3 times OrderNotFoundException at com.example.orders.OrderController.get(");
            assertThat(f.recommendation()).contains("@RestControllerAdvice");
        });
    }

    @Test
    void manyOccurrencesAreCritical() {
        String trace = """
                2026-01-01T10:00:00.000Z ERROR 1 --- [exec-1] o.a.c.c.C : Servlet.service() threw exception
                java.lang.IllegalStateException: no tenant
                \tat com.example.Tenants.current(Tenants.java:10)
                \tat org.springframework.web.servlet.FrameworkServlet.service(FrameworkServlet.java:1)
                """;
        Diagnosis diagnosis = Diagnosis.empty().withLog(
                LogParser.parse(trace.repeat(ExceptionHotspotRule.CRITICAL_OCCURRENCES)));

        assertThat(rule.evaluate(diagnosis)).singleElement().satisfies(f -> {
            assertThat(f.severity()).isEqualTo(Severity.CRITICAL);
            assertThat(f.title()).isEqualTo("IllegalStateException in Tenants.current (50 times)");
        });
    }

    @Test
    void startupCrashIsLeftToTheStartupRule() {
        assertThat(rule.evaluate(Fixtures.withLog("logs/startup-init-crash.log"))).isEmpty();
    }
}
