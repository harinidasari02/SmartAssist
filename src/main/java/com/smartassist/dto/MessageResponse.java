package com.smartassist.dto;

import com.smartassist.model.Message;
import java.time.LocalDateTime;

public record MessageResponse(Long id, String role, String content, LocalDateTime createdAt) {
    public static MessageResponse from(Message m) {
        return new MessageResponse(m.getId(), m.getRole().name(), m.getContent(), m.getCreatedAt());
    }
}
