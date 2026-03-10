package com.educaflow.base.infrastructure.mail.impl;


import jakarta.annotation.Nonnull;

public record SmtpCredentialSimplePassword(String host,String userName,String password) {

    public SmtpCredentialSimplePassword {
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("host no puede ser null ni blank");
        }
        if (userName == null || userName.isBlank()) {
            throw new IllegalArgumentException("userName no puede ser null ni blank");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("password no puede ser null ni blank");
        }
    }

}
