package dev.jdoctor.analyze.parse;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Container limits as the JVM sees them, from the cgroup section of {@code jcmd <pid> VM.info}.
 *
 * @param memoryLimitBytes empty when the JVM runs without a memory limit
 * @param activeProcessors CPUs the JVM thinks it can use
 */
public record ContainerInfo(String containerType, OptionalLong memoryLimitBytes, OptionalInt activeProcessors) {

    private static final Pattern TYPE = Pattern.compile("(?m)^container_type: (\\S+)");
    private static final Pattern MEMORY = Pattern.compile("(?m)^memory_limit_in_bytes: (\\d+)(\\s*k)?\\s*$");
    private static final Pattern CPUS = Pattern.compile("(?m)^active_processor_count: (\\d+)");

    /** Empty when VM.info has no container section (bare metal, VM, or containers disabled). */
    public static Optional<ContainerInfo> fromVmInfo(String vmInfo) {
        Matcher type = TYPE.matcher(vmInfo);
        if (!type.find()) {
            return Optional.empty();
        }
        OptionalLong memory = OptionalLong.empty();
        Matcher mem = MEMORY.matcher(vmInfo);
        if (mem.find()) {
            long value = Long.parseLong(mem.group(1));
            memory = OptionalLong.of(mem.group(2) != null ? value * 1024 : value);
        }
        Matcher cpus = CPUS.matcher(vmInfo);
        OptionalInt processors = cpus.find() ? OptionalInt.of(Integer.parseInt(cpus.group(1))) : OptionalInt.empty();
        return Optional.of(new ContainerInfo(type.group(1), memory, processors));
    }
}
