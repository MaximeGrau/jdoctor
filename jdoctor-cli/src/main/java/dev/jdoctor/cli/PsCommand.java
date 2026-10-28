package dev.jdoctor.cli;

import dev.jdoctor.collect.Jcmd;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

@Command(name = "ps", description = "Lists the JVMs running on this machine.")
final class PsCommand implements Callable<Integer> {

    @Spec
    CommandSpec spec;

    @Override
    public Integer call() {
        long self = ProcessHandle.current().pid();
        new Jcmd().list().lines()
                .filter(line -> !line.isBlank() && Character.isDigit(line.charAt(0)))
                .filter(line -> !line.startsWith(self + " "))
                .filter(line -> !line.contains("sun.tools.jcmd.JCmd"))
                .forEach(line -> spec.commandLine().getOut().println("pid:" + line));
        return 0;
    }
}
