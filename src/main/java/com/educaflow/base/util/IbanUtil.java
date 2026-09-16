package com.educaflow.base.util;

import java.math.BigInteger;
import java.util.Locale;
import java.util.regex.Pattern;

public class IbanUtil {

    private static final BigInteger MOD = BigInteger.valueOf(97);

    private static final Pattern PATTERN_IBAN_ES = Pattern.compile("ES[0-9]{22}");

    public static boolean isValid(String iban) {

        if (iban == null) {
            return false;
        }

        String val = iban.toUpperCase(Locale.ROOT).replace(" ", "").trim();

        if (!PATTERN_IBAN_ES.matcher(val).matches()) {
            return false;
        }

        // Mueve los 4 primeros caracteres (país + dígitos de control) al final
        String reordenado = val.substring(4) + val.substring(0, 4);

        // Sustituye cada letra por su valor numérico (A=10 ... Z=35)
        StringBuilder numerico = new StringBuilder(reordenado.length() * 2);
        for (int i = 0; i < reordenado.length(); i++) {
            char c = reordenado.charAt(i);
            if (Character.isDigit(c)) {
                numerico.append(c);
            } else {
                numerico.append(c - 'A' + 10);
            }
        }

        return new BigInteger(numerico.toString()).mod(MOD).equals(BigInteger.ONE);
    }

}
