package dev.jdoctor.analyze.parse;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class VmFlagsTest {

    private final VmFlags flags = new VmFlags("""
            668:
            -XX:CICompilerCount=3 -XX:MaxHeapSize=3586129920 -XX:MaxRAMPercentage=75.000000 -XX:+UseG1GC
            """);

    @Test
    void readsValuesAndGc() {
        assertThat(flags.maxHeapBytes()).hasValue(3586129920L);
        assertThat(flags.value("MaxRAMPercentage")).hasValue("75.000000");
        assertThat(flags.gcName()).isEqualTo("G1");
        assertThat(flags.longFlag("Missing")).isEmpty();
    }
}
