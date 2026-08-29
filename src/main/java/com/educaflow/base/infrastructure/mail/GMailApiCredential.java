package com.educaflow.base.infrastructure.mail;

public record GMailApiCredential(
        String clientId,
        String projectId,
        String clientSecret,
        String refreshToken
) {
    public GMailApiCredential {
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalArgumentException("clientId no puede ser null ni blank");
        }
        if (projectId == null || projectId.isBlank()) {
            throw new IllegalArgumentException("projectId no puede ser null ni blank");
        }
        if (clientSecret == null || clientSecret.isBlank()) {
            throw new IllegalArgumentException("clientSecret no puede ser null ni blank");
        }
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("refreshToken no puede ser null ni blank");
        }
    }
}
