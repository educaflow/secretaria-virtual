package com.educaflow.base.infrastructure.validation.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;

import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.i18n.I18n;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.quality.Strictness;

/**
 * Tests de {@link AlwaysFail} y {@link AlwaysPass}. Una rechaza siempre y la otra acepta siempre, sea
 * cual sea el valor; por eso los casos cubren valor presente y valor nulo. De {@code AlwaysFail} sí se
 * comprueba el texto, porque el mensaje no es un literal fijo de la regla sino lo que le pasa cada
 * llamada. {@code I18n} se mockea para que devuelva su propio argumento: sin contexto de Axelor
 * arrancado la traducción no está disponible.
 */
class AlwaysRulesTest {

    private static final String MENSAJE_SUBSANACION_POR_BOTON =
            "Para pedir una subsanación use el botón «Pedir subsanación al alumno»";
    private static final String MENSAJE_SUBSANACION_POR_SENTIDO =
            "Para pedir una subsanación elija el sentido «Pedir subsanación»";
    private static final String VALOR_CUALQUIERA = "un valor cualquiera";

    private MockedStatic<I18n> i18n;

    /** Segundo parámetro de {@code validate}: estas reglas no lo usan, ni siquiera miran el valor. */
    private final Object beanIrrelevante = new Object();

    @BeforeEach
    void mockearI18n() {
        // lenient: AlwaysPass no pasa por I18n.
        i18n = Mockito.mockStatic(I18n.class, Mockito.withSettings().strictness(Strictness.LENIENT));
        i18n.when(() -> I18n.get(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @AfterEach
    void cerrarMockDeI18n() {
        if (i18n != null) {
            i18n.close();
        }
    }

    @Test
    void alwaysFail_conValor_devuelveSiempreElMensaje() {
        BusinessMessages mensajes =
                new AlwaysFail(MENSAJE_SUBSANACION_POR_BOTON)
                        .validate(VALOR_CUALQUIERA, beanIrrelevante);

        assertMensajeUnico(MENSAJE_SUBSANACION_POR_BOTON, mensajes);
    }

    @Test
    void alwaysFail_valorNulo_devuelveElMismoMensaje() {
        BusinessMessages mensajes =
                new AlwaysFail(MENSAJE_SUBSANACION_POR_BOTON).validate(null, beanIrrelevante);

        assertMensajeUnico(MENSAJE_SUBSANACION_POR_BOTON, mensajes);
    }

    @Test
    void alwaysFail_dosInstanciasConDistintoMensaje_devuelveCadaUnaElSuyo() {
        BusinessMessages mensajesBoton =
                new AlwaysFail(MENSAJE_SUBSANACION_POR_BOTON)
                        .validate(VALOR_CUALQUIERA, beanIrrelevante);
        BusinessMessages mensajesSentido =
                new AlwaysFail(MENSAJE_SUBSANACION_POR_SENTIDO)
                        .validate(VALOR_CUALQUIERA, beanIrrelevante);

        assertMensajeUnico(MENSAJE_SUBSANACION_POR_BOTON, mensajesBoton);
        assertMensajeUnico(MENSAJE_SUBSANACION_POR_SENTIDO, mensajesSentido);
    }

    @Test
    void alwaysPass_conValor_loAcepta() {
        assertNull(new AlwaysPass().validate(VALOR_CUALQUIERA, beanIrrelevante));
    }

    @Test
    void alwaysPass_valorNulo_loAcepta() {
        assertNull(new AlwaysPass().validate(null, beanIrrelevante));
    }

    private void assertMensajeUnico(String textoEsperado, BusinessMessages mensajes) {
        assertNotNull(mensajes, "Se esperaba un mensaje para «" + textoEsperado + "», no null");
        assertEquals(
                1, mensajes.size(), "Se esperaba exactamente un mensaje para «" + textoEsperado + "»");
        assertEquals(textoEsperado, mensajes.get(0).getMessage());
    }
}
