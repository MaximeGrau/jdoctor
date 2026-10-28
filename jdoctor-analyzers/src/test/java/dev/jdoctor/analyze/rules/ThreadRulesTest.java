package dev.jdoctor.analyze.rules;

import static org.assertj.core.api.Assertions.assertThat;

import dev.jdoctor.analyze.Fixtures;
import dev.jdoctor.analyze.parse.ThreadDump;
import dev.jdoctor.analyze.parse.ThreadDumpParser;
import dev.jdoctor.core.Finding;
import dev.jdoctor.core.Severity;
import java.util.List;
import org.junit.jupiter.api.Test;

class ThreadRulesTest {

    private final ThreadDump busy = ThreadDumpParser.parse(Fixtures.resource("dumps/hikari-deadlock.txt"));
    private final ThreadDump idle = ThreadDumpParser.parse(Fixtures.resource("dumps/idle.txt"));

    @Test
    void deadlockIsCriticalWithTheThreadsInvolved() {
        List<Finding> findings = new DeadlockRule().evaluate(Fixtures.withThreadDumps(List.of(idle, busy, busy)));

        assertThat(findings).singleElement().satisfies(f -> {
            assertThat(f.id()).isEqualTo("JD-THR-001");
            assertThat(f.severity()).isEqualTo(Severity.CRITICAL);
            assertThat(f.evidence()).first().isEqualTo("Deadlock reported in 2 of 3 thread dumps");
            assertThat(f.evidence()).anyMatch(e -> e.contains("which is held by \"locker-b\""));
        });
    }

    @Test
    void noDeadlockNoFinding() {
        assertThat(new DeadlockRule().evaluate(Fixtures.withThreadDumps(List.of(idle)))).isEmpty();
    }

    @Test
    void poolSaturatedInSeveralDumpsNamesTheApplicationCaller() {
        List<Finding> findings = new HikariPoolExhaustionRule()
                .evaluate(Fixtures.withThreadDumps(List.of(busy, busy, idle)));

        assertThat(findings).singleElement().satisfies(f -> {
            assertThat(f.id()).isEqualTo("JD-SPR-001");
            assertThat(f.severity()).isEqualTo(Severity.WARNING);
            assertThat(f.evidence()).first().isEqualTo("Threads waiting for a connection, per dump: 3, 3, 0");
            assertThat(f.evidence()).contains(
                    "Waiting from com.example.orders.ReportService.monthlyReport (4 times)",
                    "Waiting from com.example.orders.OrderRepository.save (2 times)");
        });
    }

    @Test
    void callerBehindASpringProxyIsNamedAfterTheService() {
        ThreadDump.ThreadInfo thread = new ThreadDump.ThreadInfo("http-nio-8080-exec-9", "TIMED_WAITING", List.of(
                "com.zaxxer.hikari.pool.HikariPool.getConnection(HikariPool.java:180)",
                "org.springframework.transaction.interceptor.TransactionInterceptor.invoke(TransactionInterceptor.java:119)",
                "com.example.orders.ReportService$$SpringCGLIB$$0.monthlyReport(<generated>)",
                "com.example.orders.OrderController.report(OrderController.java:30)"));

        assertThat(HikariPoolExhaustionRule.applicationCaller(thread))
                .hasValue("com.example.orders.ReportService.monthlyReport");
    }

    @Test
    void oneBusyDumpOutOfThreeIsNotSaturation() {
        assertThat(new HikariPoolExhaustionRule().evaluate(Fixtures.withThreadDumps(List.of(busy, idle, idle))))
                .isEmpty();
    }
}
