package com.smartassist.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.smartassist.dto.ChatTurn;
import com.smartassist.exception.AiProviderException;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/** Talks to the xAI Grok API (OpenAI-compatible POST /chat/completions). */
@Component
public class GrokProvider implements AiProvider {
    private final RestClient client;
    private final String apiKey;
    private final String model;
    private final String systemPrompt;

    public GrokProvider(RestClient.Builder builder,
                        @Value("${app.grok.url}") String url,
                        @Value("${app.grok.api-key}") String apiKey,
                        @Value("${app.grok.model}") String model,
                        @Value("${app.ai.system-prompt}") String systemPrompt) {
        this.client = builder.baseUrl(url).build();
        this.apiKey = apiKey;
        this.model = model;
        this.systemPrompt = systemPrompt;
    }

    @Override
    public String name() { return "grok"; }

    @Override
    public String chat(List<ChatTurn> turns) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new AiProviderException("Grok isn't configured. Set the GROK_API_KEY environment variable.");
        }
        Map<String, Object> body = Map.of(
                "model", model,
                "messages", PromptMessages.build(systemPrompt, turns));
        JsonNode res;
        try {
            res = client.post().uri("/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON).body(body)
                    .retrieve().body(JsonNode.class);
        } catch (RestClientResponseException ex) {
            throw new AiProviderException("Grok API error (" + ex.getStatusCode().value()
                    + "). Check GROK_API_KEY and GROK_MODEL.", ex);
        } catch (RestClientException ex) {
            throw new AiProviderException("Couldn't reach the Grok API.", ex);
        }
        String reply = res == null ? "" : res.path("choices").path(0).path("message").path("content").asText("");
        if (reply.isBlank()) throw new AiProviderException("Grok returned an empty reply.");
        return reply;
    }
}
