package com.educaflow.base.infrastructure.validation.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.axelor.db.modelservice.BusinessMessage;
import com.axelor.db.modelservice.BusinessMessages;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

/**
 * Tests de caracterización de la función privada de nivel superior
 * {@code getBusinessMessagesUpdatingFieldNameAndLabel} de {@code FieldValidationRules.kt},
 * que reescribe el campo y la etiqueta de los mensajes de una validación anidada
 * anteponiéndoles los del campo padre (y el índice si el padre es una lista).
 */
class FieldValidationRulesKtTest {

    private static final String FIELD = "alumno";
    private static final String LABEL = "Alumno";

    private static BusinessMessages invoke(BusinessMessages messages, Integer index) throws Exception {
        Method method = FieldValidationRulesKt.class.getDeclaredMethod(
                "getBusinessMessagesUpdatingFieldNameAndLabel",
                BusinessMessages.class, String.class, String.class, Integer.class);
        method.setAccessible(true);
        return (BusinessMessages) method.invoke(null, messages, FIELD, LABEL, index);
    }

    private static BusinessMessages of(BusinessMessage... messages) {
        BusinessMessages result = new BusinessMessages();
        for (BusinessMessage message : messages) {
            result.add(message);
        }
        return result;
    }

    @Test
    void mensajesNulosDevuelveListaVacia() throws Exception {
        BusinessMessages result = invoke(null, null);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void mensajesVaciosDevuelveListaVacia() throws Exception {
        BusinessMessages result = invoke(new BusinessMessages(), 2);

        assertTrue(result.isEmpty());
    }

    @Test
    void sinCampoNiEtiquetaUsaLosDelPadre() throws Exception {
        BusinessMessages result = invoke(of(new BusinessMessage(null, "Obligatorio", null)), null);

        assertEquals(of(new BusinessMessage(FIELD, "Obligatorio", LABEL)), result);
    }

    @Test
    void campoVacioYEtiquetaEnBlancoUsaLosDelPadreAunqueHayaIndice() throws Exception {
        BusinessMessages result = invoke(of(new BusinessMessage("", "Obligatorio", "   ")), 3);

        assertEquals(of(new BusinessMessage(FIELD, "Obligatorio", LABEL)), result);
    }

    @Test
    void conCampoYEtiquetaSinIndiceLosConcatena() throws Exception {
        BusinessMessages result = invoke(of(new BusinessMessage("nombre", "Obligatorio", "Nombre")), null);

        assertEquals(of(new BusinessMessage("alumno.nombre", "Obligatorio", "Alumno Nombre")), result);
    }

    @Test
    void conCampoYEtiquetaConIndiceLosConcatenaConElIndice() throws Exception {
        BusinessMessages result = invoke(of(new BusinessMessage("nombre", "Obligatorio", "Nombre")), 1);

        assertEquals(of(new BusinessMessage("alumno[1].nombre", "Obligatorio", "Alumno [1] Nombre")), result);
    }

    @Test
    void conservaElMensajeNuloYElOrdenDeVariosMensajes() throws Exception {
        BusinessMessages result = invoke(of(
                new BusinessMessage("nombre", null, null),
                new BusinessMessage(null, "Otro", "Apellidos")), 0);

        assertEquals(of(
                new BusinessMessage("alumno[0].nombre", null, LABEL),
                new BusinessMessage(FIELD, "Otro", "Alumno [0] Apellidos")), result);
    }

    @Test
    void noModificaLosMensajesOriginales() throws Exception {
        BusinessMessages original = of(new BusinessMessage("nombre", "Obligatorio", "Nombre"));

        BusinessMessages result = invoke(original, null);

        assertEquals(of(new BusinessMessage("nombre", "Obligatorio", "Nombre")), original);
        assertNotSame(original, result);
    }
}
