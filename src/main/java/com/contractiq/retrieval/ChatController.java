package com.contractiq.retrieval;

import com.contractiq.identity.UserPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@RequestMapping("/api/v1/chat")
@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")
public class ChatController {
    private final GroundedChatService chat;

    public ChatController(GroundedChatService chat) {
        this.chat = chat;
    }

    @PostMapping
    GroundedChatService.ChatResponse ask(@AuthenticationPrincipal UserPrincipal user, @Valid @RequestBody ChatRequest request) {
        return chat.ask(user, request.question(), request.contractIds());
    }

    public record ChatRequest(@NotBlank @Size(max = 4000) String question,
                              @jakarta.validation.constraints.Size(max = 50) Set<String> contractIds) {}
}
