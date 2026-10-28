package dev.jdoctor.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.sun.net.httpserver.HttpServer;
import dev.jdoctor.core.Finding;
import dev.jdoctor.core.Severity;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** Checks the request jdoctor sends and how it reads the answer, against a local fake API. */
class AnthropicExplainerTest {

    private static final Finding FINDING = new Finding("JD-THR-001", Severity.CRITICAL, "Deadlock",
            List.of("Deadlock reported in 3 of 3 thread dumps"), "Restart.");

    private HttpServer server;
    private final AtomicReference<String> requestBody = new AtomicReference<>();
    private final AtomicReference<String> betaHeader = new AtomicReference<>();

    @AfterEach
    void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void sendsFindingWithFallbacksAndReturnsText() throws Exception {
        AnthropicExplainer explainer = explainerAnswering(message("end_turn", "## Cause\\nLock order."));

        String text = explainer.explain(FINDING, new ExplainContext(List.of("GC: G1"), List.of()));

        assertThat(text).isEqualTo("## Cause\nLock order.");
        assertThat(requestBody.get()).contains("\"model\":\"claude-opus-5-5\"", "\"fallbacks\":\"default\"",
                "JD-THR-001", "Deadlock reported in 3 of 3 thread dumps");
        assertThat(betaHeader.get()).contains("server-side-fallback-2026-07-01");
    }

    @Test
    void refusalIsAnError() throws Exception {
        AnthropicExplainer explainer = explainerAnswering(message("refusal", ""));

        assertThatThrownBy(() -> explainer.explain(FINDING, new ExplainContext(List.of(), List.of())))
                .isInstanceOf(ExplainException.class)
                .hasMessageContaining("declined");
    }

    private AnthropicExplainer explainerAnswering(String json) throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            betaHeader.set(exchange.getRequestHeaders().getFirst("anthropic-beta"));
            byte[] body = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        return new AnthropicExplainer(AnthropicOkHttpClient.builder()
                .apiKey("test-key")
                .baseUrl("http://127.0.0.1:" + server.getAddress().getPort())
                .maxRetries(0)
                .build(), AnthropicExplainer.DEFAULT_MODEL);
    }

    private static String message(String stopReason, String text) {
        String content = text.isEmpty() ? "[]" : "[{\"type\":\"text\",\"text\":\"" + text + "\"}]";
        return """
                {"id":"msg_test","type":"message","role":"assistant","model":"claude-opus-5-5",
                 "content":%s,"stop_reason":"%s","stop_sequence":null,
                 "usage":{"input_tokens":10,"output_tokens":5}}
                """.formatted(content, stopReason);
    }
}
