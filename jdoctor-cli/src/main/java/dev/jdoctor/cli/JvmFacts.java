package dev.jdoctor.cli;

import dev.jdoctor.analyze.Diagnosis;
import dev.jdoctor.analyze.Units;
import java.util.ArrayList;
import java.util.List;

/** Short context lines about the diagnosed JVM, shown in reports and given to the LLM. */
final class JvmFacts {

    private JvmFacts() {
    }

    static List<String> of(Diagnosis diagnosis) {
        List<String> facts = new ArrayList<>();
        diagnosis.flags().ifPresent(f -> facts.add("GC: " + f.gcName()));
        diagnosis.maxHeapBytes().ifPresent(b -> facts.add("Max heap: " + Units.bytes(b)));
        diagnosis.container().ifPresent(c -> {
            String limit = c.memoryLimitBytes().stream().mapToObj(Units::bytes).findFirst().orElse("none");
            facts.add("Container (" + c.containerType() + "): memory limit " + limit
                    + c.activeProcessors().stream().mapToObj(n -> ", " + n + " CPUs").findFirst().orElse(""));
        });
        diagnosis.jfr().ifPresent(j -> facts.add("JFR recording: " + j.duration().toSeconds() + " s, "
                + j.collections().size() + " GCs"));
        if (!diagnosis.threadDumps().isEmpty()) {
            facts.add("Thread dumps: " + diagnosis.threadDumps().size() + " ("
                    + diagnosis.threadDumps().get(0).threads().size() + " threads in the first)");
        }
        return facts;
    }
}
