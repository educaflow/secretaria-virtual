package com.educaflow.base.util;

import org.hibernate.validator.internal.constraintvalidators.bv.EmailValidator;

public class EMailUtil {

    private static final EmailValidator VALIDATOR = new EmailValidator();

    public static boolean isValid(String eMail) {
        if (eMail == null || eMail.isEmpty()) {
            return false;
        }
        return VALIDATOR.isValid(eMail, null);
    }

}