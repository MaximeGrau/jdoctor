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

        Result analyze = run("analyze", "pid:" + target.pid(), "--duration", "24s", "--llm", "none",
                "--format", "json", "--out", bundle.toString(), "--fail-on", "critical");

        assertThat(analyze.exitCode()).as(analyze.err()).isEqualTo(JDoctor.EXIT_FINDINGS);
        JsonNode report = new ObjectMapper().readTree(analyze.out());
        assertThat(report.get("schemaVersion").asInt()).isEqualTo(1);
        assertThat(report.get("kind").asText()).isEqualTo("jvm-diagnosis");
        List<String> ids = new ArrayList<>();
        report.get("findings").forEach(f -> ids.add(f.get("id").asText()));
        assertThat(ids).contains("JD-THR-001", "JD-MEM-001");
        assertThat(Files.list(bundle).map(p -> p.getFileName().toString()).toList())
                .contains("manifest.json", "recording.jfr", "vm-flags.txt", "vm-info.txt",
                        "thread-dump-1.txt", "thread-dump-3.txt", "heap-histogram.txt");

        Result explain = run("explain", "JD-THR-001", bundle.toString(), "--llm", "none");

        assertThat(explain.exitCode()).as(explain.err()).isZero();
        assertThat(explain.out()).startsWith("# JD-THR-001: deadlock");
    }

    @Test
    void unknownTargetIsAClearError() {
        Result result = run("analyze", "k8s:prod/api");

        assertThat(result.exitCode()).isEqualTo(1);
        assertThat(result.err()).contains("planned for v0.1");
    }

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
