package com.contractiq.analytics;

import com.contractiq.identity.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analytics")
@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")
public class AnalyticsController {
    private final AnalyticsService analytics;

    public AnalyticsController(AnalyticsService analytics) {
        this.analytics = analytics;
    }

    @GetMapping("/overview")
    AnalyticsService.Overview overview(@AuthenticationPrincipal UserPrincipal principal) {
        return analytics.overview(principal);
    }
}
