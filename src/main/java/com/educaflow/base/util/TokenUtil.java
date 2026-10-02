package com.educaflow.base.util;

import java.security.SecureRandom;
import java.util.UUID;

public class TokenUtil {
    private static final char[] ALFABETO_CSV = "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toCharArray();
    private static final SecureRandom RNG = new SecureRandom();

    public static String generateGUID() {
        return UUID.randomUUID().toString();
    }

    private static final int LONGITUD_CODIGO_VERIFICACION_SEGURO = 26;

    /**
     * @return un código aleatorio de 26 caracteres (130 bits), para lo que se protege solo con que no se pueda adivinar
     */
    public static String generateCodigoSeguroVerificacion() {
        return generateCodigoSeguroVerificacion(LONGITUD_CODIGO_VERIFICACION_SEGURO);
    }

    /**
     * @return {@code true} si el texto tiene la forma de los códigos de {@link #generateCodigoSeguroVerificacion()}; {@code false} si es {@code null}
     */
    public static boolean isCodigoSeguroVerificacion(String codigo) {
        if ((codigo == null) || (codigo.length() != LONGITUD_CODIGO_VERIFICACION_SEGURO)) {
            return false;
        }

        String alfabeto = new String(ALFABETO_CSV);

        return codigo.chars().allMatch(c -> alfabeto.indexOf(c) >= 0);
    }

    private static String generateCodigoSeguroVerificacion(int longitud) {

        byte[] b = new byte[longitud];
        RNG.nextBytes(b);
        StringBuilder sb = new StringBuilder(longitud);
        for (byte x : b) sb.append(ALFABETO_CSV[x & 0x1F]); // 256/32 exacto, sin sesgo
        return sb.toString();
    }


}
