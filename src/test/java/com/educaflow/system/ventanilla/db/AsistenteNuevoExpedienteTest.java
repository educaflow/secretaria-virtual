package com.educaflow.system.ventanilla.db;

import com.axelor.i18n.I18n;
import com.educaflow.subsystem.expedientes.db.Tramite;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

// El getter generado asigna el resultado del compute*() al campo de respaldo antes de devolverlo y
// captura la NPE del cálculo devolviendo el último valor de ese campo; por eso los casos que esperan
// null siembran antes el campo con otro valor: así el null observado solo puede venir del cálculo.
class AsistenteNuevoExpedienteTest {

    private static final String NOMBRE_TRAMITE = "Anulación de matrícula";
    private static final String AYUDA_TRAMITE = "Texto de ayuda";

    /* ------------------------------------------------------------------ */
    /* Helpers                                                            */
    /* ------------------------------------------------------------------ */

    private static Tramite tramite(String name, String help) {
        Tramite tramite = new Tramite();
        tramite.setId(10L);
        tramite.setName(name);
        tramite.setHelp(help);
        return tramite;
    }

    /* ------------------------------------------------------------------ */
    /* getNombreTramite                                                   */
    /* ------------------------------------------------------------------ */

    @Test
    void getNombreTramite_sinTramite_devuelveNull() {
        AsistenteNuevoExpediente asistente = new AsistenteNuevoExpediente();
        asistente.setNombreTramite("valor que llega del cliente");

        assertNull(asistente.getNombreTramite());
    }

    @Test
    void getNombreTramite_conTramite_devuelveElNombreTraducidoConElPrefijoValue() {
        AsistenteNuevoExpediente asistente = new AsistenteNuevoExpediente();
        asistente.setTramite(tramite(NOMBRE_TRAMITE, AYUDA_TRAMITE));

        try (MockedStatic<I18n> i18nMock = Mockito.mockStatic(I18n.class)) {
            i18nMock.when(() -> I18n.get("value:" + NOMBRE_TRAMITE)).thenReturn("Anul·lació de matrícula");

            assertEquals("Anul·lació de matrícula", asistente.getNombreTramite());

            i18nMock.verify(() -> I18n.get("value:" + NOMBRE_TRAMITE));
            i18nMock.verify(() -> I18n.get(NOMBRE_TRAMITE), never());
        }
    }

    @Test
    void getNombreTramite_conValorPrevioEnElCampo_loRecalculaYLoDescarta() {
        AsistenteNuevoExpediente asistente = new AsistenteNuevoExpediente();
        asistente.setTramite(tramite(NOMBRE_TRAMITE, AYUDA_TRAMITE));
        asistente.setNombreTramite("otro nombre");

        try (MockedStatic<I18n> i18nMock = Mockito.mockStatic(I18n.class)) {
            i18nMock.when(() -> I18n.get(any(String.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            assertEquals("value:" + NOMBRE_TRAMITE, asistente.getNombreTramite());
        }
    }

    /* ------------------------------------------------------------------ */
    /* getAyudaTramite                                                    */
    /* ------------------------------------------------------------------ */

    @Test
    void getAyudaTramite_sinTramite_devuelveNull() {
        AsistenteNuevoExpediente asistente = new AsistenteNuevoExpediente();
        asistente.setAyudaTramite("ayuda que llega del cliente");

        assertNull(asistente.getAyudaTramite());
    }

    @Test
    void getAyudaTramite_conTramite_devuelveLaAyudaDelTramite() {
        AsistenteNuevoExpediente asistente = new AsistenteNuevoExpediente();
        asistente.setTramite(tramite(NOMBRE_TRAMITE, AYUDA_TRAMITE));

        assertEquals(AYUDA_TRAMITE, asistente.getAyudaTramite());
    }

    @Test
    void getAyudaTramite_tramiteSinAyuda_devuelveNull() {
        AsistenteNuevoExpediente asistente = new AsistenteNuevoExpediente();
        asistente.setTramite(tramite(NOMBRE_TRAMITE, null));
        asistente.setAyudaTramite("ayuda vieja");

        assertNull(asistente.getAyudaTramite());
    }
}
