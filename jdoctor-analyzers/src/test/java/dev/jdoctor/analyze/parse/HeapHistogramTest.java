package dev.jdoctor.analyze.parse;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class HeapHistogramTest {

    @Test
    void parsesRowsLargestFirst() {
        String text = """
                668:
                 num     #instances         #bytes  class name (module)
                -------------------------------------------------------
                   1:         14283         837160  [B (java.base@21.0.12.1)
                   2:          3099         373480  java.lang.Class (java.base@21.0.12.1)
                Total         17382        1210640
                """;

        assertThat(HeapHistogram.parse(text)).containsExactly(
                new HeapHistogram.Entry("[B", 14283, 837160),
                new HeapHistogram.Entry("java.lang.Class", 3099, 373480));
    }
}
