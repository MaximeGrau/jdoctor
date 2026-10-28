package dev.jdoctor.analyze;

import dev.jdoctor.analyze.parse.ContainerInfo;
import dev.jdoctor.analyze.parse.HeapHistogram;
import dev.jdoctor.analyze.parse.JfrParser;
import dev.jdoctor.analyze.parse.ThreadDumpParser;
import dev.jdoctor.analyze.parse.VmFlags;
import dev.jdoctor.core.Bundle;
import java.util.List;

public final class DiagnosisLoader {

    private DiagnosisLoader() {
    }

    public static Diagnosis load(Bundle bundle) {
        return new Diagnosis(
                bundle.file(Bundle.RECORDING).map(JfrParser::parse),
                bundle.threadDumps().stream().map(ThreadDumpParser::parse).toList(),
                bundle.text(Bundle.VM_FLAGS).map(VmFlags::new),
                bundle.text(Bundle.VM_INFO).flatMap(ContainerInfo::fromVmInfo),
                bundle.text(Bundle.HEAP_HISTOGRAM).map(HeapHistogram::parse).orElse(List.of()));
    }
}
