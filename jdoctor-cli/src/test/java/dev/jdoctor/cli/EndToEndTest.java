package dev.jdoctor.cli;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

/** Collects from a real JVM with jcmd and checks that the rules find its problems. */
class EndToEndTest {

    private Process target;

    @AfterEach
    void stopTarget() {
        if (target != null) {
            target.destroyForcibly();
        }
    }

    @Test
    void findsTheLeakAndTheDeadlockInALiveJvm(@TempDir Path dir) throws Exception {
        target = startTroubledApp();
        Path bundle = dir.resolve("bundle");
        Path log = Files.writeString(dir.resolve("app.log"), NPE_LOG);

        Result analyze = run("analyze", "pid:" + target.pid(), "--duration", "24s", "--llm", "none",
                "--format", "json", "--out", bundle.toString(), "--log", log.toString(), "--fail-on", "critical");

        assertThat(analyze.exitCode()).as(analyze.err()).isEqualTo(JDoctor.EXIT_FINDINGS);
        JsonNode report = new ObjectMapper().readTree(analyze.out());
        assertThat(report.get("schemaVersion").asInt()).isEqualTo(1);
        assertThat(report.get("kind").asText()).isEqualTo("jvm-diagnosis");
        List<String> ids = new ArrayList<>();
        report.get("findings").forEach(f -> ids.add(f.get("id").asText()));
        assertThat(ids).contains("JD-THR-001", "JD-MEM-001", "JD-EXC-001");
        assertThat(Files.list(bundle).map(p -> p.getFileName().toString()).toList())
                .contains("manifest.json", "recording.jfr", "vm-flags.txt", "vm-info.txt",
                        "thread-dump-1.txt", "thread-dump-3.txt", "heap-histogram.txt", "app.log");

        Result explain = run("explain", "JD-THR-001", bundle.toString(), "--llm", "none");

        assertThat(explain.exitCode()).as(explain.err()).isZero();
        assertThat(explain.out()).startsWith("# JD-THR-001: deadlock");
    }

    @Test
    void analyzesAStartupFailureFromALogAlone(@TempDir Path dir) throws Exception {
        Path log = Files.writeString(dir.resolve("startup.log"), """
                2026-10-28T17:58:20.100Z ERROR 1 --- [main] o.s.b.d.LoggingFailureAnalysisReporter :\s

                ***************************
                APPLICATION FAILED TO START
                ***************************

                Description:

                Web server failed to start. Port 8080 was already in use.

                Action:

                Identify and stop the process that's listening on port 8080 or configure this application to listen on another port.

                """);

        Result analyze = run("analyze", log.toString(), "--llm", "none", "--fail-on", "critical");

        assertThat(analyze.exitCode()).as(analyze.err()).isEqualTo(JDoctor.EXIT_FINDINGS);
        assertThat(analyze.out()).contains("JD-START-006", "Port 8080 was already in use");
    }

    @Test
    void unknownTargetIsAClearError() {
        Result result = run("analyze", "k8s:prod/api");

        assertThat(result.exitCode()).isEqualTo(1);
        assertThat(result.err()).contains("planned for v0.1");
    }

    private static final String NPE_LOG = """
            2026-10-28T17:58:30.295Z ERROR 1 --- [exec-2] o.a.c.c.C.[.[.[/].[dispatcherServlet] : Servlet.service() threw exception
            java.lang.NullPointerException: Cannot read the array length because "order" is null
            \tat com.example.orders.OrderController.size(OrderController.java:37) ~[!/:0.0.1]
            \tat java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:103)
            """;

    private record Result(int exitCode, String out, String err) {
    }

    private static Result run(String... args) {
        StringWriter out = new StringWriter();
        StringWriter err = new StringWriter();
        CommandLine cli = JDoctor.commandLine();
        cli.setOut(new PrintWriter(out, true));
        cli.setErr(new PrintWriter(err, true));
        int code = cli.execute(args);
        return new Result(code, out.toString(), err.toString());
    }

    private static Process startTroubledApp() throws Exception {
        String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        Process process = new ProcessBuilder(java, "-Xmx512m", "-XX:+UseG1GC",
                "-cp", System.getProperty("java.class.path"), TroubledApp.class.getName())
                .redirectErrorStream(true)
                .start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
        // Skip JVM notices such as "Picked up JAVA_TOOL_OPTIONS" until the app says it is ready.
        String line;
        do {
            line = reader.readLine();
        } while (line != null && !line.equals("ready"));
        assertThat(line).as("target JVM never printed ready").isNotNull();
        return process;
    }
}
