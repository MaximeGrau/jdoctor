package dev.jdoctor.analyze.parse;

import java.util.Optional;
import java.util.OptionalLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Reads values from {@code jcmd <pid> VM.flags} output. */
public final class VmFlags {

    private final String text;

    public VmFlags(String text) {
        this.text = text;
    }

    public OptionalLong maxHeapBytes() {
        return longFlag("MaxHeapSize");
    }

    public OptionalLong longFlag(String name) {
        return value(name).map(v -> OptionalLong.of(Long.parseLong(v))).orElse(OptionalLong.empty());
    }

    public Optional<String> value(String name) {
        Matcher m = Pattern.compile("-XX:" + Pattern.quote(name) + "=(\\S+)").matcher(text);
        return m.find() ? Optional.of(m.group(1)) : Optional.empty();
    }

    /** True for boolean flags shown as {@code -XX:+Name}. */
    public boolean enabled(String name) {
        return text.contains("-XX:+" + name);
    }

    public String gcName() {
        for (String gc : new String[] {"UseG1GC", "UseZGC", "UseParallelGC", "UseSerialGC", "UseShenandoahGC"}) {
            if (enabled(gc)) {
                return gc.substring(3, gc.length() - 2);
            }
        }
        return "unknown";
    }
}
