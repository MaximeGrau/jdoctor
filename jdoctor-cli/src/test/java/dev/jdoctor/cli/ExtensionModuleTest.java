package dev.jdoctor.cli;

import static org.assertj.core.api.Assertions.assertThat;

import dev.jdoctor.core.spi.CommandProvider;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.concurrent.Callable;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Spec;

/** A module on the classpath adds its commands without any change to the CLI. */
class ExtensionModuleTest {

    public static final class HelloProvider implements CommandProvider {
        @Override
        public List<Object> commands() {
            return List.of(new HelloCommand());
        }
    }

    @Command(name = "hello", description = "Test command from an extension module.")
    static final class HelloCommand implements Callable<Integer> {
        @Spec
        CommandLine.Model.CommandSpec spec;

        @Override
        public Integer call() {
            spec.commandLine().getOut().println("hello from a module");
            return 0;
        }
    }

    @Test
    void moduleCommandIsAvailable() {
        StringWriter out = new StringWriter();
        CommandLine cli = JDoctor.commandLine();
        cli.setOut(new PrintWriter(out, true));

        assertThat(cli.execute("hello")).isZero();
        assertThat(out.toString()).contains("hello from a module");
    }
}
