package dev.jdoctor.llm;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.core.JsonValue;
import com.anthropic.models.beta.AnthropicBeta;
import com.anthropic.models.beta.messages.BetaMessage;
import com.anthropic.models.beta.messages.BetaOutputConfig;
import com.anthropic.models.beta.messages.BetaStopReason;
import com.anthropic.models.beta.messages.MessageCreateParams;
import dev.jdoctor.core.Finding;
import java.util.stream.Collectors;

/**
 * Explains findings with Claude, using the user's own API key ({@code ANTHROPIC_API_KEY}).
 *
 * <p>Requests opt into server-side fallbacks, so a refused request is retried on another model
 * instead of failing.
 */
public final class AnthropicExplainer implements Explainer {

    public static final String DEFAULT_MODEL = "claude-opus-5-5";
    private static final long MAX_TOKENS = 16_000L;

    private final AnthropicClient client;
    private final String model;

    public AnthropicExplainer(String model) {
        this(AnthropicOkHttpClient.fromEnv(), model);
    }

    AnthropicExplainer(AnthropicClient client, String model) {
        this.client = client;
        this.model = model;
    }

    @Override
    public String explain(Finding finding, ExplainContext context) {
        MessageCreateParams params = MessageCreateParams.builder()
                .model(model)
                .maxTokens(MAX_TOKENS)
                .system(Prompts.SYSTEM)
                .outputConfig(BetaOutputConfig.builder().effort(BetaOutputConfig.Effort.MEDIUM).build())
                .addBeta(AnthropicBeta.of("server-side-fallback-2026-07-01"))
                .putAdditionalBodyProperty("fallbacks", JsonValue.from("default"))
                .addUserMessage(Prompts.user(finding, context))
                .build();

        BetaMessage response = client.beta().messages().create(params);
        if (response.stopReason().filter(BetaStopReason.REFUSAL::equals).isPresent()) {
            throw new ExplainException("The model declined to explain " + finding.id() + ".");
        }
        return response.content().stream()
                .flatMap(block -> block.text().stream())
                .map(text -> text.text())
                .collect(Collectors.joining("\n"))
                .strip();
    }
}
