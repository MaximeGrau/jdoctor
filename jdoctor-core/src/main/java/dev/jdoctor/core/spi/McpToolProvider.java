package dev.jdoctor.core.spi;

import java.util.List;

/**
 * Lets each module register its own tools with the MCP server (planned for v0.1), found with
 * {@link java.util.ServiceLoader} through
 * {@code META-INF/services/dev.jdoctor.core.spi.McpToolProvider}.
 */
public interface McpToolProvider {

    List<McpTool> tools();
}
