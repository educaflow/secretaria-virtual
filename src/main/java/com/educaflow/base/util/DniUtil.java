package com.educaflow.base.util;

import java.util.regex.Pattern;

public class DniUtil {

    private static final char[] arrLettersDcDni = {'T', 'R', 'W', 'A', 'G', 'M', 'Y', 'F', 'P', 'D', 'X', 'B', 'N', 'J', 'Z', 'S', 'Q', 'V', 'H', 'L', 'C', 'K', 'E'};
    private static final char[] arrLettersDcCif = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J'};

    private static final Pattern PATTERN_CIF = Pattern.compile("[ABCDEFGHJUV][0-9]{8}");
    private static final Pattern PATTERN_CIF_OTRO = Pattern.compile("[ABCDEFGPQSNWR][0-9]{7}[ABCDEFGHIJ]");

    private static final Pattern PATTERN_DNI = Pattern.compile("[0-9]{8}[TRWAGMYFPDXBNJZSQVHLCKE]");
    private static final Pattern PATTERN_NIE = Pattern.compile("[XYZ][0-9]{7}[TRWAGMYFPDXBNJZSQVHLCKE]");
    private static final Pattern PATTERN_DNI_OTRO = Pattern.compile("[KLM][0-9]{7}[TRWAGMYFPDXBNJZSQVHLCKE]"); //Españoles no resientes sin DNI (NIF L),Españoles residentes menores 14 años sin DNI,Extranjeros sin NIE

    private static final Pattern PATTERN_DNI_PREFIXED =
            Pattern.compile("^0[0-9]{8}[A-Z]$");

    // 0YXXXXXXXL (NIE con cero antes de la letra)
    private static final Pattern PATTERN_NIE_PREFIXED =
            Pattern.compile("^0[XYZ][0-9]{7}[A-Z]$");

    // Y0XXXXXXXL (NIE con cero después de la letra)
    private static final Pattern PATTERN_NIE_INTERNAL_ZERO =
            Pattern.compile("^[XYZ]0[0-9]{7}[A-Z]$");


    public static String clean(String dni) {
        String val = ((String) dni).toUpperCase().trim();

        // Caso: 0XXXXXXXXL -> XXXXXXXXL
        if (PATTERN_DNI_PREFIXED.matcher(val).matches()) {
            return val.substring(1);
        }

        // Caso: 0YXXXXXXXL -> YXXXXXXXL
        if (PATTERN_NIE_PREFIXED.matcher(val).matches()) {
            return val.substring(1);
        }

        // Caso: Y0XXXXXXXL -> YXXXXXXXL
        if (PATTERN_NIE_INTERNAL_ZERO.matcher(val).matches()) {
            return val.charAt(0) + val.substring(2);
        }

        return val;
    }


    public static boolean isValid(String dni) {

        if (dni == null) {
            return false;
        }
        if (dni.length() != 9) {
            return false;
        }

        //Una 1º letra de CIF y el resto números
        if (PATTERN_CIF.matcher(dni).matches()) {
            int calculoDC = 0;
            for (int i = 1; i < 8; i++) {
                int digit=dni.charAt(i) - '0';
                int addValue;

                if ((i == 2) || (i == 4) || (i == 6)) {
                    addValue= digit;
                } else {
                    addValue = digit * 2;
                    if (addValue > 9) {
                        addValue -= 9;
                    }
                }

                calculoDC=calculoDC+addValue;
            }
            calculoDC = 10 - calculoDC % 10;
            if (calculoDC == 10) {
                calculoDC = 0;
            }

            if (calculoDC == dni.charAt(8) - '0') {
                return true;
            }

            //Falla el DC
            return false;
        }

        //CIF con 1º Letra, numeros y al final otra letra
        if (PATTERN_CIF_OTRO.matcher(dni).matches()) {
            int calculoDC = 0;
            for (int i = 1; i < 8; i++) {
                int digit=dni.charAt(i) - '0';
                int addValue;

                if ((i == 2) || (i == 4) || (i == 6)) {
                    addValue=digit;
                } else {
                    addValue = digit * 2;
                    if (addValue > 9) {
                        addValue -= 9;
                    }
                }

                calculoDC=calculoDC+addValue;
            }
            calculoDC = 10 - calculoDC % 10;
            if (arrLettersDcCif[calculoDC - 1] == dni.charAt(8)) {
                //CIF de organización o extranjero
                return true;
            }

            return false;
        }

        //NIF con Numeros y al final Letra de DC NIF
        if (PATTERN_DNI.matcher(dni).matches()) {
            String sNumero = dni.substring(0, 8);
            int numero = Integer.parseInt(sNumero);
            int calculoDC = numero % 23;
            if (calculoDC + 1 > 23) {
                return false;
            }
            if (dni.charAt(8) == arrLettersDcDni[calculoDC]) {
                if (dni.equalsIgnoreCase("00000001R") || dni.equalsIgnoreCase("00000000T") || dni.equalsIgnoreCase("99999999R")) {
                    //La EAET permite estos NIFs
                    return false;
                }
                return true;
            }

            return false;
        }

        //NIE con 1º Letra de NIE (X,Y,Z), despues  numeros y al final Letra de DC NIF
        if (PATTERN_NIE.matcher(dni).matches()) {
            String sNumero = dni.substring(1, 8);
            int numero = Integer.parseInt(sNumero);
            if (dni.charAt(0) == 'Y') {
                numero += 10000000;
            } else if (dni.charAt(0) == 'Z') {
                numero += 20000000;
            }
            int calculoDC = numero % 23;
            calculoDC += 1;
            if (calculoDC > 23) {
                return false;
            }
            if (dni.charAt(8) == arrLettersDcDni[(calculoDC - 1)]) {
                if (dni.equals("X0000000T")) {
                    //Este dni nunca existe
                    return false;
                }

                return true;
            }

            return false;
        }

        //NIF ESPECIAL 1º Letra (K,L,M), despues  numeros y al final Letra de DC NIF
        if (PATTERN_DNI_OTRO.matcher(dni).matches()) {
            String sNumero = dni.substring(1, 3);
            int numero = Integer.parseInt(sNumero);
            if ((numero < 1) || (numero > 56)) {
                return false;
            }

            sNumero = dni.substring(1, 8);
            numero = Integer.parseInt(sNumero);
            int calculoDC = numero % 23;
            calculoDC += 1;
            if (calculoDC > 23) {
                return false;
            }
            if (dni.charAt(8) == arrLettersDcDni[calculoDC - 1]) {
                return true;
            }

            return false;
        }

        return false;
    }

}
