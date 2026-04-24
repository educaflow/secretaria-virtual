package com.educaflow.base.util;

import java.util.UUID;

public class TokenUtil {

    public static String generar() {
        return UUID.randomUUID().toString();
    }
}
