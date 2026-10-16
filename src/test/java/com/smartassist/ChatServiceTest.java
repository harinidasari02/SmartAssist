package com.smartassist;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

import com.smartassist.ai.AiProvider;
import com.smartassist.dto.ChatRequest;
import com.smartassist.dto.ChatResponse;
import com.smartassist.dto.ChatTurn;
import com.smartassist.exception.AiProviderException;
import com.smartassist.exception.BadRequestException;
import com.smartassist.exception.ResourceNotFoundException;
import com.smartassist.model.*;
import com.smartassist.repository.ConversationRepository;
import com.smartassist.repository.MessageRepository;
import com.smartassist.service.ChatService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {
    @Mock ConversationRepository conversations;
    @Mock MessageRepository messages;
    @Mock AiProvider ollama;
    @Captor ArgumentCaptor<List<ChatTurn>> turnsCaptor;

    ChatService service;

    @BeforeEach
    void setUp() {
        when(ollama.name()).thenReturn("ollama");
        service = new ChatService(conversations, messages, List.of(ollama));
    }

    private Message message(Role role, String content) {
        Message m = new Message();
        m.setRole(role);
        m.setContent(content);
        return m;
    }

    @Test
    void newChatCreatesConversationAndStoresBothMessages() {
        when(conversations.save(any(Conversation.class))).thenAnswer(inv -> {
            Conversation c = inv.getArgument(0);
            c.setId(7L);
            return c;
        });
        when(messages.findTop20ByConversationIdOrderByIdDesc(7L)).thenReturn(List.of());
        when(ollama.chat(anyList())).thenReturn("Hello!");

        ChatResponse res = service.chat(new ChatRequest(null, "Hi", "ollama"));

        assertEquals(7L, res.conversationId());
        assertEquals("Hello!", res.reply());
        assertEquals("ollama", res.provider());
        verify(messages, times(2)).save(any(Message.class));
    }

    @Test
    void historyIsSentToProviderOldestFirstWithNewMessageLast() {
        Conversation conv = new Conversation();
        conv.setId(3L);
        when(conversations.findById(3L)).thenReturn(Optional.of(conv));
        // repository returns newest first
        when(messages.findTop20ByConversationIdOrderByIdDesc(3L)).thenReturn(List.of(
                message(Role.ASSISTANT, "A1"), message(Role.USER, "Q1")));
        when(ollama.chat(anyList())).thenReturn("A2");

        service.chat(new ChatRequest(3L, "Q2", "ollama"));

        verify(ollama).chat(turnsCaptor.capture());
        List<ChatTurn> sent = turnsCaptor.getValue();
        assertEquals(List.of("Q1", "A1", "Q2"), sent.stream().map(ChatTurn::content).toList());
        assertEquals(Role.USER, sent.get(2).role());
    }

    @Test
    void missingProviderDefaultsToOllama() {
        when(conversations.save(any(Conversation.class))).thenAnswer(inv -> {
            Conversation c = inv.getArgument(0);
            c.setId(1L);
            return c;
        });
        when(messages.findTop20ByConversationIdOrderByIdDesc(1L)).thenReturn(List.of());
        when(ollama.chat(anyList())).thenReturn("ok");
        assertEquals("ollama", service.chat(new ChatRequest(null, "Hi", null)).provider());
    }

    @Test
    void unknownProviderIsRejected() {
        assertThrows(BadRequestException.class, () -> service.chat(new ChatRequest(null, "Hi", "skynet")));
        verifyNoInteractions(conversations);
    }

    @Test
    void unknownConversationIsNotFound() {
        when(conversations.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.chat(new ChatRequest(99L, "Hi", "ollama")));
    }

    @Test
    void providerFailureSavesNoMessages() {
        when(conversations.save(any(Conversation.class))).thenAnswer(inv -> {
            Conversation c = inv.getArgument(0);
            c.setId(5L);
            return c;
        });
        when(messages.findTop20ByConversationIdOrderByIdDesc(5L)).thenReturn(List.of());
        when(ollama.chat(anyList())).thenThrow(new AiProviderException("down"));

        assertThrows(AiProviderException.class, () -> service.chat(new ChatRequest(null, "Hi", "ollama")));
        verify(messages, never()).save(any());
    }
}
