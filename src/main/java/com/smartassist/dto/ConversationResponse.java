package com.smartassist.dto;

import com.smartassist.model.Conversation;
import java.time.LocalDateTime;

public record ConversationResponse(Long id, String title, String provider, LocalDateTime createdAt) {
    public static ConversationResponse from(Conversation c) {
        return new ConversationResponse(c.getId(), c.getTitle(), c.getProvider(), c.getCreatedAt());
    }
}
