package com.educaflow.common.junit;

import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

public class JUnitHelper {

    public static void assertThrowsCause(Class<? extends Throwable> expectedType, Executable executable) {
        Throwable ex = assertThrows(Throwable.class, executable);

        Throwable cause = ex;
        while (cause != null) {
            if (expectedType.isInstance(cause)) {
                return ;
            }
            cause = cause.getCause();
        }

        fail("No se encontró excepción de tipo " + expectedType.getName() +
                " en la cadena de causas de " + ex);
    }

}
