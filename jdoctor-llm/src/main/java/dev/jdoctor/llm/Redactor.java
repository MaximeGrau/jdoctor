package dev.jdoctor.llm;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Removes values that look personal or secret before anything leaves the machine.
 *
 * <p>Findings already contain summaries, not raw dumps; this is a second line of defence for
 * thread names, class names and stack frames that embed data.
 */
public final class Redactor {

    private record Replacement(Pattern pattern, String with) {
    }

    private static final List<Replacement> RULES = List.of(
            new Replacement(Pattern.compile("[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+"), "<email>"),
            new Replacement(Pattern.compile("eyJ[\\w-]{8,}\\.[\\w-]{8,}\\.[\\w-]{8,}"), "<jwt>"),
            new Replacement(Pattern.compile("(?i)(bearer\\s+)[\\w.~+/=-]{8,}"), "$1<token>"),
            new Replacement(Pattern.compile("(?i)((password|passwd|secret|token|api[_-]?key)\\s*[=:]\\s*)\\S+"),
                    "$1<redacted>"),
            new Replacement(Pattern.compile("\\b(\\d{1,3}\\.){3}\\d{1,3}\\b"), "<ip>"),
            new Replacement(Pattern.compile("(?i)jdbc:[a-z0-9]+://[^\\s,;)]+"), "jdbc:<url>"));

    private Redactor() {
    }

    public static String redact(String text) {
        String result = text;
        for (Replacement rule : RULES) {
            result = rule.pattern().matcher(result).replaceAll(rule.with());
        }
        return result;
    }
}
