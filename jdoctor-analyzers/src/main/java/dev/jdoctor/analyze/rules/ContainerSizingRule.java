package dev.jdoctor.analyze.rules;

import dev.jdoctor.analyze.Diagnosis;
import dev.jdoctor.analyze.Rule;
import dev.jdoctor.analyze.Units;
import dev.jdoctor.analyze.parse.ContainerInfo;
import dev.jdoctor.core.Finding;
import dev.jdoctor.core.Severity;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;

/**
 * JD-SIZE-001: the max heap leaves too little of the container memory limit for everything else.
 *
 * <p>The JVM also uses memory outside the heap (metaspace, thread stacks, code cache, direct
 * buffers, GC structures). When the heap takes almost all of the limit, the kernel kills the
 * container ({@code OOMKilled}) before the JVM can throw an OutOfMemoryError.
 */
public final class ContainerSizingRule implements Rule {

    public static final String ID = "JD-SIZE-001";

    static final double WARNING_RATIO = 0.80;
    static final double CRITICAL_RATIO = 0.90;

    @Override
    public List<Finding> evaluate(Diagnosis diagnosis) {
        OptionalLong limit = diagnosis.container().map(ContainerInfo::memoryLimitBytes).orElse(OptionalLong.empty());
        OptionalLong maxHeap = diagnosis.maxHeapBytes();
        if (limit.isEmpty() || maxHeap.isEmpty()) {
            return List.of();
        }
        double ratio = maxHeap.getAsLong() / (double) limit.getAsLong();
        if (ratio < WARNING_RATIO) {
            return List.of();
        }
        Severity severity = ratio >= CRITICAL_RATIO ? Severity.CRITICAL : Severity.WARNING;

        List<String> evidence = new ArrayList<>();
        evidence.add("Max heap " + Units.bytes(maxHeap.getAsLong()) + " = " + Units.percent(ratio)
                + " of the container limit " + Units.bytes(limit.getAsLong()));
        evidence.add("Left for metaspace, thread stacks, code cache and direct buffers: "
                + Units.bytes(Math.max(0, limit.getAsLong() - maxHeap.getAsLong())));
        diagnosis.flags().flatMap(f -> f.value("MaxRAMPercentage"))
                .ifPresent(p -> evidence.add("MaxRAMPercentage=" + p));

        return List.of(new Finding(ID, severity,
                "Max heap uses " + Units.percent(ratio) + " of the container memory: OOMKilled risk",
                evidence,
                "Replace a fixed -Xmx with -XX:MaxRAMPercentage=75, or lower -Xmx so that at least 25 % of the"
                        + " limit stays outside the heap. Check the real non-heap usage with"
                        + " -XX:NativeMemoryTracking=summary and jcmd <pid> VM.native_memory."));
    }
}
