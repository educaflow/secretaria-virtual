package com.educaflow.base.util;

import java.util.UUID;

public class CodigoVerificacionUtil {

    public static String generar() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }
}
