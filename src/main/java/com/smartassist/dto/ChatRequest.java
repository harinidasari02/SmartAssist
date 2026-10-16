package com.smartassist.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** conversationId is null for a new chat; provider defaults to "ollama". */
public record ChatRequest(
        Long conversationId,
        @NotBlank(message = "Message is required") @Size(max = 4000, message = "Message can be at most 4000 characters") String message,
        String provider) {}
