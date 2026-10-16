package com.smartassist.controller;

import com.smartassist.dto.ChatRequest;
import com.smartassist.dto.ChatResponse;
import com.smartassist.service.ChatService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ChatController {
    private final ChatService service;

    @PostMapping("/chat")
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) { return service.chat(request); }

    @GetMapping("/providers")
    public List<String> providers() { return service.providerNames(); }
}
