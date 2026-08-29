package com.educaflow.base.infrastructure.mail;


public record UserPasswordCredential(String host, String userName, String password) {

    public UserPasswordCredential {
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
