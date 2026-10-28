package dev.jdoctor.analyze;

import dev.jdoctor.analyze.parse.ContainerInfo;
import dev.jdoctor.analyze.parse.HeapHistogram;
import dev.jdoctor.analyze.parse.JfrSummary;
import dev.jdoctor.analyze.parse.ThreadDump;
import dev.jdoctor.analyze.parse.VmFlags;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

public final class Fixtures {

    private Fixtures() {
    }

    public static String resource(String name) {
        try (InputStream in = Fixtures.class.getResourceAsStream("/" + name)) {
            if (in == null) {
                throw new IllegalArgumentException("Missing test resource " + name);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static Diagnosis withJfr(JfrSummary jfr) {
        return new Diagnosis(Optional.of(jfr), List.of(), Optional.empty(), Optional.empty(), List.of());
    }

    public static Diagnosis withThreadDumps(List<ThreadDump> dumps) {
        return new Diagnosis(Optional.empty(), dumps, Optional.empty(), Optional.empty(), List.of());
    }

    public static Diagnosis withSizing(String flags, ContainerInfo container) {
        return new Diagnosis(Optional.empty(), List.of(), Optional.of(new VmFlags(flags)), Optional.of(container),
                List.<HeapHistogram.Entry>of());
    }
}
