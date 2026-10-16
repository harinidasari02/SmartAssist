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
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestClientException;

/** Talks to a local Ollama server (POST /api/chat). */
@Component
public class OllamaProvider implements AiProvider {
    private final RestClient client;
    private final String url;
    private final String model;
    private final String systemPrompt;

    public OllamaProvider(RestClient.Builder builder,
                          @Value("${app.ollama.url}") String url,
                          @Value("${app.ollama.model}") String model,
                          @Value("${app.ai.system-prompt}") String systemPrompt) {
        this.client = builder.baseUrl(url).build();
        this.url = url;
        this.model = model;
        this.systemPrompt = systemPrompt;
    }

    @Override
    public String name() { return "ollama"; }

    @Override
    public String chat(List<ChatTurn> turns) {
        Map<String, Object> body = Map.of(
                "model", model,
                "messages", PromptMessages.build(systemPrompt, turns),
                "stream", false);
        JsonNode res;
        try {
            res = client.post().uri("/api/chat").contentType(MediaType.APPLICATION_JSON).body(body)
                    .retrieve().body(JsonNode.class);
        } catch (RestClientResponseException ex) {
            throw new AiProviderException("Ollama returned an error (" + ex.getStatusCode().value()
                    + "). Check that the model '" + model + "' is pulled.", ex);
        } catch (RestClientException ex) {
            throw new AiProviderException("Couldn't reach Ollama at " + url + ". Is it running?", ex);
        }
        String reply = res == null ? "" : res.path("message").path("content").asText("");
        if (reply.isBlank()) throw new AiProviderException("Ollama returned an empty reply.");
        return reply;
    }
}
