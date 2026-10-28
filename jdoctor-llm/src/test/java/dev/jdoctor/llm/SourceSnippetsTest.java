package dev.jdoctor.llm;

import static org.assertj.core.api.Assertions.assertThat;

import dev.jdoctor.core.Finding;
import dev.jdoctor.core.Severity;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SourceSnippetsTest {

    @TempDir
    Path project;

    private final Finding finding = new Finding("JD-EXC-001", Severity.WARNING, "NPE",
            List.of("5 times NullPointerException at com.example.orders.OrderController.size(OrderController.java:40): x",
                    "1 time IllegalStateException at org.acme.Missing.run(Missing.java:3)"),
            "Fix it.");

    @Test
    void showsTheLinesAroundTheFrameInAModule() throws IOException {
        Path file = project.resolve("orders/src/main/java/com/example/orders/OrderController.java");
        Files.createDirectories(file.getParent());
        Files.writeString(file, IntStream.rangeClosed(1, 60).mapToObj(n -> "line " + n)
                .collect(Collectors.joining("\n")));

        List<SourceSnippets.Snippet> snippets = SourceSnippets.forFinding(finding, project);

        assertThat(snippets).singleElement().satisfies(s -> {
            assertThat(s.location()).isEqualTo("com/example/orders/OrderController.java:40");
            assertThat(s.code().lines().toList())
                    .hasSize(2 * SourceSnippets.CONTEXT_LINES + 1)
                    .contains(">   40  line 40", "    25  line 25", "    55  line 55");
        });
    }

    @Test
    void promptCarriesTheSnippet() {
        String prompt = Prompts.user(finding, new ExplainContext(List.of(), List.of(),
                List.of(new SourceSnippets.Snippet("com/example/A.java:3", ">    3  String token = \"abcdef\";\n"))));

        assertThat(prompt).contains("## Source code", "com/example/A.java:3", "```java");
    }

    @Test
    void nothingWhenTheSourceIsNotThere() {
        assertThat(SourceSnippets.forFinding(finding, project)).isEmpty();
    }
}
