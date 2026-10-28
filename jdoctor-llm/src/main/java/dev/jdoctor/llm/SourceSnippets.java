package dev.jdoctor.llm;

import dev.jdoctor.core.Finding;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Finds the source lines that a finding's evidence points at, such as
 * {@code com.example.Foo.bar(Foo.java:12)}, so the LLM can propose a code change.
 *
 * <p>Only the lines around each frame are read, never whole files, and the prompt that carries
 * them is redacted like the rest.
 */
public final class SourceSnippets {

    /** Lines shown before and after the line a frame points at. */
    static final int CONTEXT_LINES = 15;
    static final int MAX_SNIPPETS = 3;
    private static final int MAX_SEARCH_DEPTH = 12;

    private static final Pattern FRAME = Pattern.compile(
            "\\b((?:[a-z_][\\w]*\\.)+)[A-Z][\\w$]*\\.[\\w$<>]+\\((\\w+\\.(?:java|kt)):(\\d+)\\)");

    private SourceSnippets() {
    }

    /**
     * @param root a source root such as {@code src/main/java}, or a project directory whose
     *             modules contain source roots
     */
    public static List<Snippet> forFinding(Finding finding, Path root) {
        Set<String> seen = new LinkedHashSet<>();
        List<Snippet> snippets = new ArrayList<>();
        for (String evidence : finding.evidence()) {
            Matcher m = FRAME.matcher(evidence);
            while (m.find() && snippets.size() < MAX_SNIPPETS) {
                String relative = m.group(1).replace('.', '/') + m.group(2);
                int line = Integer.parseInt(m.group(3));
                if (seen.add(relative + ":" + line)) {
                    locate(root, relative).flatMap(file -> read(file, relative, line)).ifPresent(snippets::add);
                }
            }
        }
        return snippets;
    }

    /** The file at {@code root/relative}, else the first file under root that ends with it. */
    static Optional<Path> locate(Path root, String relative) {
        Path direct = root.resolve(relative);
        if (Files.isRegularFile(direct)) {
            return Optional.of(direct);
        }
        Path suffix = Path.of(relative);
        try (Stream<Path> files = Files.find(root, MAX_SEARCH_DEPTH,
                (path, attributes) -> attributes.isRegularFile() && path.endsWith(suffix))) {
            return files.filter(path -> !path.toString().contains("/build/")).findFirst();
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot search sources in " + root, e);
        }
    }

    private static Optional<Snippet> read(Path file, String relative, int line) {
        List<String> lines;
        try {
            lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return Optional.empty();
        }
        if (line < 1 || line > lines.size()) {
            return Optional.empty();
        }
        int from = Math.max(1, line - CONTEXT_LINES);
        int to = Math.min(lines.size(), line + CONTEXT_LINES);
        StringBuilder sb = new StringBuilder();
        for (int n = from; n <= to; n++) {
            sb.append(n == line ? ">" : " ").append(String.format("%5d  ", n)).append(lines.get(n - 1)).append('\n');
        }
        return Optional.of(new Snippet(relative + ":" + line, sb.toString()));
    }

    /**
     * @param location source path and line, such as {@code com/example/Foo.java:12}
     * @param code     numbered lines, the pointed line marked with {@code >}
     */
    public record Snippet(String location, String code) {
    }
}
