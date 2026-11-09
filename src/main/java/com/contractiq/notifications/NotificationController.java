package com.contractiq.notifications;

import com.contractiq.identity.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")
public class NotificationController {
    private final NotificationService notifications;

    public NotificationController(NotificationService notifications) {
        this.notifications = notifications;
    }

    @GetMapping
    List<NotificationService.NotificationDto> list(@AuthenticationPrincipal UserPrincipal principal) {
        return notifications.list(principal);
    }

    @PatchMapping("/{notificationId}/read")
    NotificationService.NotificationDto read(@AuthenticationPrincipal UserPrincipal principal, @PathVariable String notificationId) {
        return notifications.markRead(principal, notificationId);
    }
}
