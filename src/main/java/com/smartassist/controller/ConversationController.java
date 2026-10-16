package com.smartassist.controller;

import com.smartassist.dto.ConversationResponse;
import com.smartassist.dto.MessageResponse;
import com.smartassist.service.ChatService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ConversationController {
    private final ChatService service;

    @GetMapping
    public List<ConversationResponse> list() { return service.listConversations(); }

    @GetMapping("/{id}/messages")
    public List<MessageResponse> messages(@PathVariable Long id) { return service.history(id); }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { service.delete(id); }
}
