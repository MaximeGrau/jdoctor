package dev.jdoctor.analyze.parse;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ContainerInfoTest {

    @Test
    void readsLimitInKilobytesAsPrintedByJdk21() {
        String vmInfo = """
                container (cgroup) information:
                container_type: cgroupv1
                active_processor_count: 4
                memory_limit_in_bytes: 14005748 k
                memory_and_swap_limit_in_bytes: unlimited
                """;

        ContainerInfo info = ContainerInfo.fromVmInfo(vmInfo).orElseThrow();

        assertThat(info.containerType()).isEqualTo("cgroupv1");
        assertThat(info.memoryLimitBytes()).hasValue(14005748L * 1024);
        assertThat(info.activeProcessors()).hasValue(4);
    }

    @Test
    void unlimitedMemoryHasNoLimit() {
        ContainerInfo info = ContainerInfo.fromVmInfo("container_type: cgroupv2\nmemory_limit_in_bytes: unlimited\n")
                .orElseThrow();

        assertThat(info.memoryLimitBytes()).isEmpty();
    }

    @Test
    void noContainerSection() {
        assertThat(ContainerInfo.fromVmInfo("OS: Linux\n")).isEmpty();
    }
}
