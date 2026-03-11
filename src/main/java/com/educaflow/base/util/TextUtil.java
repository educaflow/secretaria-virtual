package com.educaflow.base.util;

import com.axelor.common.Inflector;

import java.text.Normalizer;
import java.util.Set;

public class TextUtil {
    public static String humanize(String screamingSnakeCase) {
        return Inflector.getInstance().humanize(screamingSnakeCase);
    }
    private static final Set<String> WINDOWS_RESERVED = Set.of(
            "CON", "PRN", "AUX", "NUL",
            "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9",
            "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9"
    );

    public static String toFirstsLetterToUpperCase(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }



    public static String sanitizeFileName(String input) {
        if (input == null || input.isBlank()) {
            throw new RuntimeException("El nombre de archivo no puede ser nulo o vacío");
        }

        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");

        String result = normalized
                .replaceAll("[/\\\\]", "_")              // separadores de ruta
                .replaceAll("\\s+", "_")               // espacios
                .replaceAll("[\\p{Cntrl}:*?\"<>|]", "") // peligrosos
                .replaceAll("[^a-zA-Z0-9._-]", "");

        if (result.isBlank()) {
            result = "file";
        }

        if (WINDOWS_RESERVED.contains(result.toUpperCase())) {
            result = "_" + result;
        }

        return result.length() > 255 ? result.substring(0, 255) : result;
    }

}
