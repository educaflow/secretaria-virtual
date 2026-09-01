package com.educaflow.base.util;

import java.security.SecureRandom;
import java.util.UUID;

public class TokenUtil {
    private static final char[] A = "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toCharArray();
    private static final SecureRandom RNG = new SecureRandom();

    public static String generateGUID() {
        return UUID.randomUUID().toString();
    }

    public static String generateCodigoVerificacionSeguro() {
        return generateCodigoVerificacionSeguro(26);
    }

    private static String generateCodigoVerificacionSeguro(int longitud) {

        byte[] b = new byte[longitud];
        RNG.nextBytes(b);
        StringBuilder sb = new StringBuilder(longitud);
        for (byte x : b) sb.append(A[x & 0x1F]); // 256/32 exacto, sin sesgo
        return sb.toString();
    }


}
