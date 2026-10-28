package dev.jdoctor.core.spi;

import java.util.Objects;
import java.util.function.Function;

/**
 * One tool exposed to coding agents by the jdoctor MCP server.
 *
 * @param name        unique tool name, prefixed by module, for example {@code jvm_analyze}
 * @param description what the tool does, written for the model that calls it
 * @param inputSchema JSON Schema of the arguments, as a JSON string
 * @param handler     receives the arguments as a JSON string and returns the result as text,
 *                    usually a {@link dev.jdoctor.core.Report} in JSON
 */
public record McpTool(String name, String description, String inputSchema, Function<String, String> handler) {

    public McpTool {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(inputSchema, "inputSchema");
        Objects.requireNonNull(handler, "handler");
    }
}
