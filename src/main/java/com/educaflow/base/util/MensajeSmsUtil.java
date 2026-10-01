package com.educaflow.base.util;

public class MensajeSmsUtil {

    private static final int MAXIMO_UNIDADES_GSM7 = 160;
    private static final int MAXIMO_CARACTERES_UCS2 = 70;

    // Alfabeto GSM-7 por defecto y su tabla de extensión según 3GPP TS 23.038.
    private static final String BASICO = "@£$¥èéùìòÇ\nØø\rÅå" + "Δ_ΦΓΛΩΠΨΣΘΞ" + "ÆæßÉ"
            + " !\"#¤%&'()*+,-./" + "0123456789:;<=>?"
            + "¡ABCDEFGHIJKLMNO" + "PQRSTUVWXYZÄÖÑÜ§"
            + "¿abcdefghijklmno" + "pqrstuvwxyzäöñüà";
    private static final String EXTENDIDO = "\f^{}\\[~]|€";

    public static boolean cabeEnUnSms(String mensaje) {
        TextUtil.requireNonBlank(mensaje, "El mensaje del SMS es obligatorio");
        return esCodificableEnGsm7(mensaje)
                ? contarUnidadesGsm7(mensaje) <= MAXIMO_UNIDADES_GSM7
                : mensaje.length() <= MAXIMO_CARACTERES_UCS2;
    }

    private static boolean esCodificableEnGsm7(String mensaje) {
        return mensaje.chars().allMatch(c -> BASICO.indexOf(c) >= 0 || EXTENDIDO.indexOf(c) >= 0);
    }

    private static int contarUnidadesGsm7(String mensaje) {
        return mensaje.chars().map(c -> EXTENDIDO.indexOf(c) >= 0 ? 2 : 1).sum();
    }
}
