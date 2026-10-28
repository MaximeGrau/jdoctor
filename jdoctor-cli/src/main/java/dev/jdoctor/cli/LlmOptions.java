package dev.jdoctor.cli;

import dev.jdoctor.llm.AnthropicExplainer;
import dev.jdoctor.llm.Explainer;
import dev.jdoctor.core.Finding;
import dev.jdoctor.llm.SourceSnippets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import picocli.CommandLine.Option;

/** Which LLM explains the findings. The key always comes from the user's environment. */
final class LlmOptions {

    enum Provider { ANTHROPIC, NONE }

    @Option(names = "--llm", defaultValue = "anthropic", description = "LLM provider: ${COMPLETION-CANDIDATES} (default: ${DEFAULT-VALUE}).")
    Provider provider;

    @Option(names = "--model", description = "Model id (default: $JDOCTOR_MODEL or " + AnthropicExplainer.DEFAULT_MODEL + ").")
    String model;

    @Option(names = "--source", paramLabel = "DIR", description = {
        "Application sources (src/main/java or the project directory).",
        "The LLM then sees the lines around each frame in the evidence and can propose a code fix."})
    Path source;

    /** Source lines the finding points at; empty without --source. */
    List<SourceSnippets.Snippet> snippets(Finding finding) {
        if (source == null) {
            return List.of();
        }
        if (!Files.isDirectory(source)) {
            throw new IllegalArgumentException("--source " + source + " is not a directory.");
        }
        return SourceSnippets.forFinding(finding, source);
    }

    Optional<Explainer> explainer() {
        if (provider == Provider.NONE) {
            return Optional.empty();
        }
        if (System.getenv("ANTHROPIC_API_KEY") == null) {
            throw new IllegalStateException("ANTHROPIC_API_KEY is not set. Export your key, or use --llm none.");
        }
        String chosen = model != null ? model
                : Optional.ofNullable(System.getenv("JDOCTOR_MODEL")).orElse(AnthropicExplainer.DEFAULT_MODEL);
        return Optional.of(new AnthropicExplainer(chosen));
    }
}
