package com.smartassist.service;

import com.smartassist.ai.AiProvider;
import com.smartassist.dto.*;
import com.smartassist.exception.BadRequestException;
import com.smartassist.exception.ResourceNotFoundException;
import com.smartassist.model.*;
import com.smartassist.repository.ConversationRepository;
import com.smartassist.repository.MessageRepository;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChatService {
    private static final String DEFAULT_PROVIDER = "ollama";

    private final ConversationRepository conversations;
    private final MessageRepository messages;
    private final Map<String, AiProvider> providers;

    public ChatService(ConversationRepository conversations, MessageRepository messages, List<AiProvider> providerList) {
        this.conversations = conversations;
        this.messages = messages;
        this.providers = providerList.stream().collect(Collectors.toMap(AiProvider::name, p -> p, (a, b) -> a, TreeMap::new));
    }

    public List<String> providerNames() { return new ArrayList<>(providers.keySet()); }

    /**
     * Routes the message to the chosen provider with the conversation history as context.
     * Nothing is saved if the provider fails, because the transaction rolls back.
     */
    @Transactional
    public ChatResponse chat(ChatRequest r) {
        String key = (r.provider() == null || r.provider().isBlank()) ? DEFAULT_PROVIDER : r.provider().trim().toLowerCase();
        AiProvider provider = providers.get(key);
        if (provider == null) {
            throw new BadRequestException("Unknown provider '" + key + "'. Available: " + String.join(", ", providers.keySet()) + ".");
        }

        Conversation conv = r.conversationId() == null
                ? newConversation(r.message(), provider.name())
                : conversations.findById(r.conversationId())
                        .orElseThrow(() -> new ResourceNotFoundException("Conversation not found: " + r.conversationId()));

        List<Message> recent = new ArrayList<>(messages.findTop20ByConversationIdOrderByIdDesc(conv.getId()));
        Collections.reverse(recent); // oldest first
        List<ChatTurn> turns = new ArrayList<>();
        for (Message m : recent) turns.add(new ChatTurn(m.getRole(), m.getContent()));
        turns.add(new ChatTurn(Role.USER, r.message()));

        String reply = provider.chat(turns);

        messages.save(newMessage(conv, Role.USER, r.message()));
        messages.save(newMessage(conv, Role.ASSISTANT, reply));
        return new ChatResponse(conv.getId(), provider.name(), reply);
    }

    public List<ConversationResponse> listConversations() {
        return conversations.findAllByOrderByCreatedAtDescIdDesc().stream().map(ConversationResponse::from).toList();
    }

    public List<MessageResponse> history(Long conversationId) {
        if (!conversations.existsById(conversationId)) {
            throw new ResourceNotFoundException("Conversation not found: " + conversationId);
        }
        return messages.findByConversationIdOrderByIdAsc(conversationId).stream().map(MessageResponse::from).toList();
    }

    @Transactional
    public void delete(Long conversationId) {
        Conversation c = conversations.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found: " + conversationId));
        messages.deleteByConversationId(conversationId);
        conversations.delete(c);
    }

    private Conversation newConversation(String firstMessage, String provider) {
        Conversation c = new Conversation();
        String title = firstMessage.strip();
        c.setTitle(title.length() > 60 ? title.substring(0, 60) + "..." : title);
        c.setProvider(provider);
        return conversations.save(c);
    }

    private Message newMessage(Conversation conv, Role role, String content) {
        Message m = new Message();
        m.setConversation(conv);
        m.setRole(role);
        m.setContent(content);
        return m;
    }
}
