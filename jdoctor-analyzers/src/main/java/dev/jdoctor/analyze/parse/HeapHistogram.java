package dev.jdoctor.analyze.parse;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parses {@code jcmd <pid> GC.class_histogram}. Lines come sorted by bytes, largest first. */
public final class HeapHistogram {

    private static final Pattern ROW = Pattern.compile("^\\s*\\d+:\\s+(\\d+)\\s+(\\d+)\\s+(\\S+).*$");

    private HeapHistogram() {
    }

    public record Entry(String className, long instances, long bytes) {
    }

    public static List<Entry> parse(String text) {
        List<Entry> entries = new ArrayList<>();
        for (String line : text.split("\\R")) {
            Matcher m = ROW.matcher(line);
            if (m.matches()) {
                entries.add(new Entry(m.group(3), Long.parseLong(m.group(1)), Long.parseLong(m.group(2))));
            }
        }
        return entries;
    }
}
