package com.educaflow.base.infrastructure.mail;

import com.educaflow.base.util.TextUtil;

public record GMailApiCredential(
        String clientId,
        String projectId,
        String clientSecret,
        String refreshToken
) {
    public GMailApiCredential {
        TextUtil.requireNonBlank(clientId, "clientId no puede ser null ni blank");
        TextUtil.requireNonBlank(projectId, "projectId no puede ser null ni blank");
        TextUtil.requireNonBlank(clientSecret, "clientSecret no puede ser null ni blank");
        TextUtil.requireNonBlank(refreshToken, "refreshToken no puede ser null ni blank");
    }

    @Override
    public String toString() {
        return "GMailApiCredential[clientId=" + clientId + ", projectId=" + projectId + ", clientSecret=****, refreshToken=****]";
    }
}
