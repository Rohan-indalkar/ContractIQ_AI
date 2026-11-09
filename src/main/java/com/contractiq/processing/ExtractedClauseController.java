package com.contractiq.processing;

import com.contractiq.identity.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/contracts/{contractId}/clauses")
@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")
public class ExtractedClauseController {
    private final ExtractedClauseService clauses;

    public ExtractedClauseController(ExtractedClauseService clauses) {
        this.clauses = clauses;
    }

    @GetMapping
    List<ExtractedClauseService.ClauseDto> list(@AuthenticationPrincipal UserPrincipal principal,
                                                @PathVariable String contractId,
                                                @RequestParam(required = false) String type) {
        return clauses.list(principal, contractId, type);
    }
}
