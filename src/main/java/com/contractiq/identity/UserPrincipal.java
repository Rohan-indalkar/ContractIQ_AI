package com.contractiq.identity;

public record UserPrincipal(String userId, String organizationId, String email, Role role) {
}
