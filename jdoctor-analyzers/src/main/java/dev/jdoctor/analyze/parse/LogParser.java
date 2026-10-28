package dev.jdoctor.analyze.parse;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses an application log: Logback or Log4j2 lines with stack traces, as printed by Spring Boot.
 * It does not depend on the log line format; it only looks for exception headers, {@code at}
 * frames and Spring Boot's failure report.
 */
public final class LogParser {

    /** Also matches nested types such as {@code HttpClientErrorException$TooManyRequests}. */
    private static final Pattern EXCEPTION = Pattern.compile(
            "^(?:Caused by: |Exception in thread \"[^\"]*\" )?"
                    + "((?:[a-zA-Z_$][\\w$]*\\.)+[A-Z][\\w$]*(?:Exception|Error|Throwable)(?:\\$[A-Z]\\w*)*)"
                    + "(?::\\s?(.*))?$");
    private static final Pattern FRAME = Pattern.compile("^\\tat (?:[\\w.]+/)?(\\S+\\([^)]*\\))");
    private static final Pattern NESTED = Pattern.compile("^\\t(?:\\t|Suppressed: )");
    /** A new log record starts with a date, as in Spring Boot's default patterns. */
    private static final Pattern LOG_RECORD = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}[T ]\\d{2}:\\d{2}");
    private static final String FAILED_BANNER = "APPLICATION FAILED TO START";
    private static final String RUN_FAILED = "Application run failed";
    private static final int MAX_MESSAGE_LINES = 15;

    private LogParser() {
    }

    public static LogSummary parse(Path file) {
        try {
            // Not Files.readString: a log can hold bytes that are not valid UTF-8.
            return parse(new String(Files.readAllBytes(file), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read log " + file, e);
        }
    }

    public static LogSummary parse(String text) {
        List<String> lines = text.lines().toList();
        List<LogSummary.LoggedException> exceptions = new ArrayList<>();
        Optional<LogSummary.StartupFailure> failure = Optional.empty();
        boolean startupContext = false;

        int i = 0;
        while (i < lines.size()) {
            String line = lines.get(i);
            if (LOG_RECORD.matcher(line).find()) {
                startupContext = line.contains(RUN_FAILED);
            }
            if (line.strip().equals(FAILED_BANNER)) {
                int end = endOfRecord(lines, i + 1);
                failure = Optional.of(failureReport(lines.subList(i + 1, end)));
                i = end;
                continue;
            }
            Matcher header = EXCEPTION.matcher(line);
            if (header.matches() && !line.startsWith("Caused by: ") && startsTrace(lines, i + 1)) {
                List<LogSummary.Cause> chain = new ArrayList<>();
                i = readChain(lines, i, chain);
                exceptions.add(new LogSummary.LoggedException(chain, startupContext));
                continue;
            }
            i++;
        }
        return new LogSummary(failure, exceptions);
    }

    /** Reads an exception header, its frames and each "Caused by" section; returns the next line. */
    private static int readChain(List<String> lines, int start, List<LogSummary.Cause> chain) {
        int i = start;
        while (i < lines.size()) {
            Matcher header = EXCEPTION.matcher(lines.get(i));
            if (!header.matches() || (i > start && !lines.get(i).startsWith("Caused by: "))) {
                break;
            }
            String type = header.group(1);
            StringBuilder message = new StringBuilder(header.group(2) != null ? header.group(2).strip() : "");
            int firstFrame = firstFrame(lines, i + 1);
            if (firstFrame < 0) {
                break;
            }
            // Some messages span several lines before the first frame.
            for (int m = i + 1; m < firstFrame; m++) {
                if (!lines.get(m).isBlank()) {
                    message.append(' ').append(lines.get(m).strip());
                }
            }
            List<String> frames = new ArrayList<>();
            i = firstFrame;
            while (i < lines.size()) {
                String line = lines.get(i);
                Matcher frame = FRAME.matcher(line);
                if (frame.find()) {
                    frames.add(frame.group(1));
                } else if (!NESTED.matcher(line).find() && !line.startsWith("\t...")) {
                    break;
                }
                i++;
            }
            chain.add(new LogSummary.Cause(type, message.toString().strip(), frames));
        }
        return i;
    }

    private static boolean startsTrace(List<String> lines, int index) {
        return firstFrame(lines, index) >= 0;
    }

    /** Index of the first {@code at} frame within a few lines, or -1 if another record starts first. */
    private static int firstFrame(List<String> lines, int from) {
        for (int i = from; i < Math.min(lines.size(), from + MAX_MESSAGE_LINES); i++) {
            String line = lines.get(i);
            if (FRAME.matcher(line).find()) {
                return i;
            }
            if (LOG_RECORD.matcher(line).find() || EXCEPTION.matcher(line).matches()) {
                return -1;
            }
        }
        return -1;
    }

    private static int endOfRecord(List<String> lines, int from) {
        int i = from;
        while (i < lines.size() && !LOG_RECORD.matcher(lines.get(i)).find()) {
            i++;
        }
        return i;
    }

    private static LogSummary.StartupFailure failureReport(List<String> lines) {
        StringBuilder description = new StringBuilder();
        StringBuilder action = new StringBuilder();
        StringBuilder current = null;
        for (String line : lines) {
            if (line.strip().equals("Description:")) {
                current = description;
            } else if (line.strip().equals("Action:")) {
                current = action;
            } else if (current != null && !line.startsWith("****")) {
                current.append(line).append('\n');
            }
        }
        return new LogSummary.StartupFailure(description.toString().strip(), action.toString().strip());
    }
}
