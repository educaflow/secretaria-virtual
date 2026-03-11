package com.educaflow.base.infrastructure.importer.validators.impl;

import com.educaflow.base.infrastructure.importer.validators.DniValidator;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import java.util.regex.Pattern;

@Named
@Singleton
public class DniValidatorImpl implements DniValidator {

    private static final String DNI_LETTERS = "TRWAGMYFPDXBNJZSQVHLCKE";

    private static final Pattern DNI_PATTERN =
            Pattern.compile("^([0-9]{8}|0[0-9]{8})[A-Z]$");

    private static final Pattern NIE_PATTERN =
            Pattern.compile("^([XYZ][0-9]{7}|[XYZ]0[0-9]{7})[A-Z]$");

    private static final Pattern CIF_PATTERN =
            Pattern.compile("^[ABCDEFGHJNPQRSUVW][0-9]{7}[0-9A-J]$");



    public boolean isValid(String value) {

        if (value == null || value.isBlank()) return false;


        String id = value.toUpperCase().trim();

        if (DNI_PATTERN.matcher(id).matches()) {
            return validateDni(id);
        }
        if (NIE_PATTERN.matcher(id).matches()) {
            return validateNie(id);
        }
        if (CIF_PATTERN.matcher(id).matches()) {
            return validateCif(id);
        }
        return false;
    }

    // ---------------- DNI ----------------

    private static boolean validateDni(String dni) {

        // Si el documento empieza por 0, eliminamos el 0 para evitar problemas de formato
        if(dni.startsWith("0") && dni.length() == 10) {
            dni = dni.substring(1);
        }
        int number = Integer.parseInt(dni.substring(0, 8));
        char expectedLetter = DNI_LETTERS.charAt(number % 23);
        return dni.charAt(8) == expectedLetter;
    }

    // ---------------- NIE ----------------

    private static boolean validateNie(String nie) {

        // Si el documento es un NIE con 0 después de la letra, eliminamos el 0 para evitar problemas de formato
        if (nie.length() == 10 && nie.charAt(1) == '0') {
            nie = nie.charAt(0) + nie.substring(2);
        }

        char prefix = nie.charAt(0);
        int mappedPrefix = switch (prefix) {
            case 'X' -> 0;
            case 'Y' -> 1;
            case 'Z' -> 2;
            default -> -1;
        };

        int number = Integer.parseInt(mappedPrefix + nie.substring(1, 8));
        char expectedLetter = DNI_LETTERS.charAt(number % 23);
        return nie.charAt(8) == expectedLetter;
    }

    // ---------------- CIF ----------------

    private static boolean validateCif(String cif) {
        int sumEven = 0;
        int sumOdd = 0;

        for (int i = 1; i <= 7; i++) {
            int digit = Character.getNumericValue(cif.charAt(i));

            if (i % 2 == 0) {
                sumEven += digit;
            } else {
                int tmp = digit * 2;
                sumOdd += tmp / 10 + tmp % 10;
            }
        }

        int controlDigit = (10 - ((sumEven + sumOdd) % 10)) % 10;
        char controlChar = "JABCDEFGHI".charAt(controlDigit);
        char lastChar = cif.charAt(8);

        return lastChar == Character.forDigit(controlDigit, 10)
                || lastChar == controlChar;
    }
}