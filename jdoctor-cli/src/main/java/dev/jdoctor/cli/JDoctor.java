package dev.jdoctor.cli;

import dev.jdoctor.core.Version;
import dev.jdoctor.core.spi.CommandProvider;
import dev.jdoctor.core.spi.Modules;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(
        name = "jdoctor",
        mixinStandardHelpOptions = true,
        versionProvider = JDoctor.VersionProvider.class,
        description = "Diagnoses a running JVM: memory leaks, GC pressure, deadlocks, pool saturation, container sizing.",
        subcommands = {
            PsCommand.class,
            AnalyzeCommand.class,
            CollectCommand.class,
            ExplainCommand.class,
            CommandLine.HelpCommand.class
        })
public final class JDoctor {

    /** Exit code when findings reach the --fail-on threshold. */
    static final int EXIT_FINDINGS = 2;

    public static void main(String[] args) {
        System.exit(commandLine().execute(args));
    }

    static CommandLine commandLine() {
        CommandLine cli = new CommandLine(new JDoctor());
        // Extension modules (for example jdoctor ai) add their own command families.
        for (CommandProvider provider : Modules.commandProviders()) {
            provider.commands().forEach(cli::addSubcommand);
        }
        return cli
                .setCaseInsensitiveEnumValuesAllowed(true)
                .registerConverter(java.time.Duration.class, new DurationConverter())
                .setExecutionExceptionHandler((ex, cmd, parseResult) -> {
                    cmd.getErr().println(cmd.getColorScheme().errorText("Error: " + ex.getMessage()));
                    return 1;
                });
    }

    static final class VersionProvider implements CommandLine.IVersionProvider {
        @Override
        public String[] getVersion() {
            return new String[] {"jdoctor " + Version.current(), "Java " + Runtime.version()};
        }
    }
}
