package dev.jdoctor.analyze.parse;

import static org.assertj.core.api.Assertions.assertThat;

import dev.jdoctor.analyze.Fixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class LogParserTest {

    @ParameterizedTest
    @ValueSource(strings = {"duplicate-bean", "two-candidates", "missing-bean", "cycle", "port-in-use"})
    void readsSpringBootFailureReport(String name) {
        LogSummary log = LogParser.parse(Fixtures.resource("logs/startup-" + name + ".log"));

        assertThat(log.startupFailure()).hasValueSatisfying(failure -> {
            assertThat(failure.description()).isNotBlank().doesNotContain("Description:", "Action:");
            assertThat(failure.action()).isNotBlank().doesNotContain("Action:");
        });
    }

    @Test
    void unanalyzedCrashIsAStartupExceptionWithItsCauseChain() {
        LogSummary log = LogParser.parse(Fixtures.resource("logs/startup-init-crash.log"));

        assertThat(log.startupFailure()).isEmpty();
        assertThat(log.exceptions()).singleElement().satisfies(e -> {
            assertThat(e.duringStartup()).isTrue();
            assertThat(e.chain().get(0).type())
                    .isEqualTo("org.springframework.beans.factory.BeanCreationException");
            assertThat(e.rootCause().type()).isEqualTo("java.lang.NullPointerException");
            assertThat(e.rootCause().frames()).first().asString()
                    .startsWith("com.example.orders.failures.CrashOnStartup.loadDefaults(CrashOnStartup.java:");
        });
    }

    @Test
    void runtimeExceptionsAreNotStartupOnes() {
        LogSummary log = LogParser.parse(Fixtures.resource("logs/runtime-errors.log"));

        assertThat(log.startupFailure()).isEmpty();
        assertThat(log.exceptions()).hasSize(8).noneMatch(LogSummary.LoggedException::duringStartup);
        assertThat(log.exceptions()).filteredOn(e -> e.rootCause().simpleType().equals("NullPointerException"))
                .hasSize(5)
                .allSatisfy(e -> assertThat(e.rootCause().message())
                        .isEqualTo("Cannot read the array length because \"order\" is null"));
    }

    @Test
    void framesLoseModuleAndJarDecorations() {
        LogSummary log = LogParser.parse("""
                2026-01-01T10:00:00.000Z ERROR 1 --- [main] c.e.Foo : boom
                java.lang.IllegalStateException: first line
                second line
                \tat java.base/java.util.Objects.requireNonNull(Objects.java:233)
                \tat com.example.Foo.bar(Foo.java:12) ~[app.jar:1.0]
                \t... 3 common frames omitted
                Caused by: java.io.IOException: disk
                \tat com.example.Disk.write(Disk.java:7)
                \tSuppressed: java.lang.RuntimeException: ignored
                \t\tat com.example.Disk.close(Disk.java:9)
                """);

        assertThat(log.exceptions()).singleElement().satisfies(e -> {
            assertThat(e.chain()).hasSize(2);
            assertThat(e.chain().get(0).message()).isEqualTo("first line second line");
            assertThat(e.chain().get(0).frames())
                    .containsExactly("java.util.Objects.requireNonNull(Objects.java:233)", "com.example.Foo.bar(Foo.java:12)");
            assertThat(e.rootCause().frames()).containsExactly("com.example.Disk.write(Disk.java:7)");
        });
    }

    @Test
    void nestedExceptionTypesAreRecognized() {
        LogSummary log = LogParser.parse("""
                2026-01-01T10:00:00.000Z ERROR 1 --- [exec-1] o.a.c.c.C : Servlet.service() threw exception
                org.springframework.web.client.HttpClientErrorException$TooManyRequests: 429 Too Many Requests: "{}"
                \tat org.springframework.web.client.HttpClientErrorException.create(HttpClientErrorException.java:137)
                \tat com.example.Pricing.fetch(Pricing.java:22)
                """);

        assertThat(log.exceptions()).singleElement().satisfies(e -> {
            assertThat(e.rootCause().type())
                    .isEqualTo("org.springframework.web.client.HttpClientErrorException$TooManyRequests");
            assertThat(e.rootCause().message()).startsWith("429 Too Many Requests");
        });
    }

    @Test
    void exceptionNamesInPlainLogLinesAreIgnored() {
        LogSummary log = LogParser.parse("""
                2026-01-01T10:00:00.000Z  WARN 1 --- [main] c.e.Foo : Resolved [java.lang.IllegalArgumentException: x]
                java.lang.IllegalArgumentException: not followed by frames
                """);

        assertThat(log.exceptions()).isEmpty();
    }
}
