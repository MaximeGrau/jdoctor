package dev.jdoctor.analyze.rules;

import dev.jdoctor.analyze.Diagnosis;
import dev.jdoctor.analyze.Rule;
import dev.jdoctor.analyze.parse.ThreadDump;
import dev.jdoctor.core.Finding;
import dev.jdoctor.core.Severity;
import java.util.ArrayList;
import java.util.List;

/** JD-THR-001: the JVM reports a deadlock in at least one thread dump. */
public final class DeadlockRule implements Rule {

    public static final String ID = "JD-THR-001";

    static final int MAX_REPORT_LINES = 12;

    @Override
    public List<Finding> evaluate(Diagnosis diagnosis) {
        List<ThreadDump> withDeadlock = diagnosis.threadDumps().stream().filter(ThreadDump::hasDeadlock).toList();
        if (withDeadlock.isEmpty()) {
            return List.of();
        }

        List<String> evidence = new ArrayList<>();
        evidence.add("Deadlock reported in " + withDeadlock.size() + " of " + diagnosis.threadDumps().size()
                + " thread dumps");
        withDeadlock.get(0).deadlock().lines()
                .filter(line -> !line.isBlank() && !line.startsWith("="))
                .limit(MAX_REPORT_LINES)
                .map(String::strip)
                .forEach(evidence::add);

        return List.of(new Finding(ID, Severity.CRITICAL, "Threads are deadlocked and will never recover",
                evidence,
                "Deadlocked threads only recover with a restart. Fix the code so that every thread takes the"
                        + " locks in the same order, or use ReentrantLock.tryLock with a timeout."));
    }
}
