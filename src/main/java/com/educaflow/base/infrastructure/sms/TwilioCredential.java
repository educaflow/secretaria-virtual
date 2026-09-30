package com.educaflow.base.infrastructure.sms;

import com.educaflow.base.util.TextUtil;

public record TwilioCredential(String accountSid, String authToken) {

    public TwilioCredential {
        TextUtil.requireNonBlank(accountSid, "accountSid no puede ser null ni blank");
        TextUtil.requireNonBlank(authToken, "authToken no puede ser null ni blank");
    }

    @Override
    public String toString() {
        return "TwilioCredential[accountSid=" + accountSid + ", authToken=****]";
    }
}
