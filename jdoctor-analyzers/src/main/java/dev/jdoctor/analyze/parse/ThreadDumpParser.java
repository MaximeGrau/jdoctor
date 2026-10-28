package dev.jdoctor.analyze.parse;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parses the text printed by {@code jcmd <pid> Thread.print} or {@code jstack}. */
public final class ThreadDumpParser {

    private static final Pattern HEADER = Pattern.compile("^\"(.*?)\"\\s.*");
    private static final Pattern STATE = Pattern.compile("^\\s+java\\.lang\\.Thread\\.State: (\\w+).*");
    private static final Pattern FRAME = Pattern.compile("^\\s+at (.+)$");
    private static final String DEADLOCK_START = "Found one Java-level deadlock";
    private static final Pattern DEADLOCK_END = Pattern.compile("^Found \\d+ deadlocks?\\.");

    private ThreadDumpParser() {
    }

    public static ThreadDump parse(String text) {
        List<ThreadDump.ThreadInfo> threads = new ArrayList<>();
        StringBuilder deadlock = new StringBuilder();

        String name = null;
        String state = "UNKNOWN";
        List<String> frames = new ArrayList<>();
        boolean inDeadlock = false;

        for (String line : text.split("\\R")) {
            if (line.startsWith(DEADLOCK_START)) {
                inDeadlock = true;
            }
            if (inDeadlock) {
                deadlock.append(line).append('\n');
                if (DEADLOCK_END.matcher(line).matches()) {
                    inDeadlock = false;
                }
                continue;
            }

            Matcher header = HEADER.matcher(line);
            if (header.matches()) {
                if (name != null) {
                    threads.add(new ThreadDump.ThreadInfo(name, state, frames));
                }
                name = header.group(1);
                state = "UNKNOWN";
                frames = new ArrayList<>();
                continue;
            }
            if (name == null) {
                continue;
            }
            Matcher stateMatch = STATE.matcher(line);
            if (stateMatch.matches()) {
                state = stateMatch.group(1);
                continue;
            }
            Matcher frame = FRAME.matcher(line);
            if (frame.matches()) {
                frames.add(frame.group(1));
            }
        }
        if (name != null) {
            threads.add(new ThreadDump.ThreadInfo(name, state, frames));
        }
        return new ThreadDump(threads, deadlock.toString().strip());
    }
}
