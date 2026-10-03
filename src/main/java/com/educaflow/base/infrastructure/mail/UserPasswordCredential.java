package com.educaflow.base.infrastructure.mail;

import com.educaflow.base.util.TextUtil;


public record UserPasswordCredential(String host, String userName, String password) {

    public UserPasswordCredential {
        TextUtil.requireNonBlank(host, "host no puede ser null ni blank");
        TextUtil.requireNonBlank(userName, "userName no puede ser null ni blank");
        TextUtil.requireNonBlank(password, "password no puede ser null ni blank");
    }

    @Override
    public String toString() {
        return "UserPasswordCredential[host=" + host + ", userName=" + userName + ", password=****]";
    }
}
