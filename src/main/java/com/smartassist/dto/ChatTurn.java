package com.smartassist.dto;

import com.smartassist.model.Role;

/** One message in the context window sent to an AI provider. */
public record ChatTurn(Role role, String content) {}
