package com.smartassist.ai;

import com.smartassist.dto.ChatTurn;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Builds the {role, content} message list that both Ollama and Grok (OpenAI-style) APIs accept. */
final class PromptMessages {
    private PromptMessages() {}

    static List<Map<String, String>> build(String systemPrompt, List<ChatTurn> turns) {
        List<Map<String, String>> out = new ArrayList<>();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            out.add(Map.of("role", "system", "content", systemPrompt));
        }
        for (ChatTurn t : turns) {
            out.add(Map.of("role", t.role().name().toLowerCase(), "content", t.content()));
        }
        return out;
    }
}
