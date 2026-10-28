package dev.jdoctor.collect;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/** Runs {@code jcmd} against a local JVM. No agent is installed in the target. */
public final class Jcmd {

    private static final long TIMEOUT_SECONDS = 60;

    private final String executable;

    public Jcmd() {
        this(locate());
    }

    Jcmd(String executable) {
        this.executable = executable;
    }

    /** Prefers the jcmd of the JDK running jdoctor, then whatever is on the PATH. */
    private static String locate() {
        Path sibling = Path.of(System.getProperty("java.home"), "bin", "jcmd");
        return Files.isExecutable(sibling) ? sibling.toString() : "jcmd";
    }

    public String run(long pid, String... command) {
        List<String> args = new ArrayList<>();
        args.add(executable);
        args.add(Long.toString(pid));
        args.addAll(List.of(command));
        return exec(args);
    }

    /** Lists the JVMs jcmd can see, one {@code pid main-class} pair per line. */
    public String list() {
        return exec(List.of(executable, "-l"));
    }

    /** Drops the "Picked up JAVA_TOOL_OPTIONS: ..." lines that launchers print on stderr. */
    static String withoutLauncherNotices(String output) {
        return output.lines()
                .filter(line -> !line.startsWith("Picked up "))
                .collect(Collectors.joining("\n", "", "\n"));
    }

    private static String exec(List<String> args) {
        try {
            Process process = new ProcessBuilder(args).redirectErrorStream(true).start();
            byte[] out = process.getInputStream().readAllBytes();
            if (!process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new CollectionException("Timed out: " + String.join(" ", args));
            }
            String output = withoutLauncherNotices(new String(out, StandardCharsets.UTF_8));
            // jcmd prints "<pid>:" then either the answer or an exception, sometimes with exit code 0.
            Optional<String> error = output.lines()
                    .filter(line -> !line.isBlank() && !line.matches("^\\d+:$"))
                    .findFirst()
                    .filter(line -> line.matches("^[\\w.]+(Exception|Error)\\b.*"));
            if (process.exitValue() != 0 || error.isPresent()) {
                throw new CollectionException("jcmd " + String.join(" ", args.subList(1, args.size())) + ": "
                        + error.orElse(output.strip()));
            }
            return output;
        } catch (IOException e) {
            throw new CollectionException("Cannot run jcmd. Is a JDK (not only a JRE) installed?", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CollectionException("Interrupted while running jcmd", e);
        }
    }
}
