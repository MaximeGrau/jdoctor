package dev.jdoctor.core.spi;

import java.util.List;
import java.util.ServiceLoader;

/** Finds the extension modules on the classpath. */
public final class Modules {

    private Modules() {
    }

    public static List<CommandProvider> commandProviders() {
        return ServiceLoader.load(CommandProvider.class).stream().map(ServiceLoader.Provider::get).toList();
    }

    public static List<McpToolProvider> mcpToolProviders() {
        return ServiceLoader.load(McpToolProvider.class).stream().map(ServiceLoader.Provider::get).toList();
    }
}
