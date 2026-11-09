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

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/search")
@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")
public class RetrievalController {
    private final RetrievalService retrieval;

    public RetrievalController(RetrievalService retrieval) {
        this.retrieval = retrieval;
    }

    @PostMapping("/semantic")
    List<TenantVectorIndex.Evidence> search(@AuthenticationPrincipal UserPrincipal user, @Valid @RequestBody SearchRequest request) {
        return retrieval.search(user, request.query(), request.contractIds(), request.limit());
    }

    public record SearchRequest(@NotBlank @Size(max = 4000) String query,
                                @jakarta.validation.constraints.Size(max = 50) Set<String> contractIds,
                                @jakarta.validation.constraints.Min(1) @jakarta.validation.constraints.Max(20) int limit) {}
}
