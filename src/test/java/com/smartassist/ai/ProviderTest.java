package com.smartassist.ai;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import com.smartassist.dto.ChatTurn;
import com.smartassist.exception.AiProviderException;
import com.smartassist.model.Role;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class ProviderTest {
    private final List<ChatTurn> turns = List.of(new ChatTurn(Role.USER, "Hello"));

    @Test
    void ollamaParsesReplyAndSendsModelAndMessages() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OllamaProvider provider = new OllamaProvider(builder, "http://localhost:11434", "llama3.2", "Be brief.");

        server.expect(requestTo("http://localhost:11434/api/chat"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.model").value("llama3.2"))
                .andExpect(jsonPath("$.stream").value(false))
                .andExpect(jsonPath("$.messages[0].role").value("system"))
                .andExpect(jsonPath("$.messages[1].content").value("Hello"))
                .andRespond(withSuccess("{\"message\":{\"role\":\"assistant\",\"content\":\"Hi there\"}}", MediaType.APPLICATION_JSON));

        assertEquals("Hi there", provider.chat(turns));
        server.verify();
    }

    @Test
    void ollamaServerErrorBecomesAiProviderException() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OllamaProvider provider = new OllamaProvider(builder, "http://localhost:11434", "llama3.2", "Be brief.");
        server.expect(requestTo("http://localhost:11434/api/chat")).andRespond(withServerError());
        assertThrows(AiProviderException.class, () -> provider.chat(turns));
    }

    @Test
    void grokSendsBearerKeyAndParsesReply() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        GrokProvider provider = new GrokProvider(builder, "https://api.x.ai/v1", "test-key", "grok-4", "Be brief.");

        server.expect(requestTo("https://api.x.ai/v1/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-key"))
                .andExpect(jsonPath("$.model").value("grok-4"))
                .andRespond(withSuccess("{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"Hello from Grok\"}}]}", MediaType.APPLICATION_JSON));

        assertEquals("Hello from Grok", provider.chat(turns));
        server.verify();
    }

    @Test
    void grokWithoutApiKeyFailsBeforeAnyRequest() {
        GrokProvider provider = new GrokProvider(RestClient.builder(), "https://api.x.ai/v1", "", "grok-4", "Be brief.");
        AiProviderException ex = assertThrows(AiProviderException.class, () -> provider.chat(turns));
        assertTrue(ex.getMessage().contains("GROK_API_KEY"));
    }

    @Test
    void grokEmptyReplyIsAnError() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        GrokProvider provider = new GrokProvider(builder, "https://api.x.ai/v1", "k", "grok-4", "Be brief.");
        server.expect(requestTo("https://api.x.ai/v1/chat/completions"))
                .andRespond(withSuccess("{\"choices\":[]}", MediaType.APPLICATION_JSON));
        assertThrows(AiProviderException.class, () -> provider.chat(turns));
    }
}
