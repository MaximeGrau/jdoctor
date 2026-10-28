package dev.jdoctor.cli;

import dev.jdoctor.collect.Target;
import dev.jdoctor.core.Bundle;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

@Command(name = "collect", description = "Collects a bundle from a live JVM, to analyze later or elsewhere.")
final class CollectCommand implements Callable<Integer> {

    @Spec
    CommandSpec spec;

    @Parameters(index = "0", paramLabel = "TARGET", description = "pid:<pid> or <pid>.")
    String target;

    @Option(names = {"-o", "--out"}, description = "Bundle directory (default: ./jdoctor-bundle-<pid>-<time>).")
    Path out;

    @Mixin
    CollectionOptions collection;

    @Override
    public Integer call() {
        if (!(Target.parse(target) instanceof Target.LocalPid pid)) {
            throw new IllegalArgumentException("collect needs a live JVM: pid:<pid>");
        }
        Bundle bundle = Targets.collect(pid, collection.toOptions(), out, spec.commandLine().getErr());
        spec.commandLine().getOut().println(bundle.dir());
        return 0;
    }
}
