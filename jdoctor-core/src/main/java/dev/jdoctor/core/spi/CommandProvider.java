package dev.jdoctor.core.spi;

import java.util.List;

/**
 * Adds a family of commands to the CLI without changing its core, for example {@code jdoctor ai}.
 *
 * <p>Implementations are found with {@link java.util.ServiceLoader}: list the class in
 * {@code META-INF/services/dev.jdoctor.core.spi.CommandProvider} and put the jar on the classpath.
 * Command ids, such as finding ids, should use a prefix of their own ({@code JD-AI-...}).
 */
public interface CommandProvider {

    /**
     * Instances of picocli {@code @Command} classes, added as top-level subcommands. Typed as
     * {@code Object} so that the core does not depend on picocli.
     */
    List<Object> commands();
}
