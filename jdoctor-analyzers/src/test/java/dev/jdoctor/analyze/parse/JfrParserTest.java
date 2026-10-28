package dev.jdoctor.analyze.parse;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import jdk.jfr.Recording;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JfrParserTest {

    @Test
    void readsCollectionsAndHeapAfterGcFromARealRecording(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("test.jfr");
        try (Recording recording = new Recording()) {
            recording.enable("jdk.GarbageCollection");
            recording.enable("jdk.GCHeapSummary");
            recording.enable("jdk.GCHeapConfiguration");
            recording.start();
            List<byte[]> garbage = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                garbage.add(new byte[1 << 20]);
                System.gc();
            }
            recording.stop();
            recording.dump(file);
            assertThat(garbage).hasSize(3);
        }

        JfrSummary summary = JfrParser.parse(file);

        assertThat(summary.collections()).hasSizeGreaterThanOrEqualTo(3);
        assertThat(summary.collections()).anyMatch(gc -> gc.cause().equals("System.gc()"));
        assertThat(summary.heapAfterGc()).isNotEmpty().allMatch(s -> s.usedBytes() > 0);
        assertThat(summary.maxHeapBytes()).isPresent();
        assertThat(summary.end()).isAfterOrEqualTo(summary.start());
    }
}
