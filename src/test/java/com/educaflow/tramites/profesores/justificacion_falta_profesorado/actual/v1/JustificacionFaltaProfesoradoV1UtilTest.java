package com.educaflow.tramites.profesores.justificacion_falta_profesorado.actual.v1;

import com.axelor.auth.db.User;
import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.i18n.I18n;
import com.educaflow.base.infrastructure.validation.messages.BusinessException;
import com.educaflow.base.util.SecurityUtil;
import com.educaflow.subsystem.expedientes.db.JustificacionFaltaProfesoradoV1;
import com.educaflow.subsystem.expedientes.db.TipoJornadaFaltaJustificacionFaltaProfesoradoV1;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;

class JustificacionFaltaProfesoradoV1UtilTest {

    private static final String MENSAJE_PRESENTAR = "Solo puede presentar sus propias solicitudes";
    private static final String MENSAJE_BORRAR = "Solo puede borrar sus propias solicitudes";

    /* ------------------------------------------------------------------ */
    /* Helpers                                                            */
    /* ------------------------------------------------------------------ */

    private static JustificacionFaltaProfesoradoV1 conTipoJornada(
            TipoJornadaFaltaJustificacionFaltaProfesoradoV1 tipoJornadaFalta) {
        JustificacionFaltaProfesoradoV1 expediente = new JustificacionFaltaProfesoradoV1();
        expediente.setTipoJornadaFalta(tipoJornadaFalta);
        return expediente;
    }

    /* ------------------------------------------------------------------ */
    /* necesitaFechaFin                                                   */
    /* ------------------------------------------------------------------ */

    @Test
    void necesitaFechaFin_clasificaCadaTipoDeJornada() {
        assertAll(
                () -> assertFalse(JustificacionFaltaProfesoradoV1Util.necesitaFechaFin(
                        conTipoJornada(TipoJornadaFaltaJustificacionFaltaProfesoradoV1.UN_DIA_COMPLETO))),
                () -> assertTrue(JustificacionFaltaProfesoradoV1Util.necesitaFechaFin(
                        conTipoJornada(TipoJornadaFaltaJustificacionFaltaProfesoradoV1.VARIOS_DIAS_COMPLETOS))),
                () -> assertFalse(JustificacionFaltaProfesoradoV1Util.necesitaFechaFin(
                        conTipoJornada(TipoJornadaFaltaJustificacionFaltaProfesoradoV1.UNAS_HORAS_UN_DIA))),
                () -> assertTrue(JustificacionFaltaProfesoradoV1Util.necesitaFechaFin(
                        conTipoJornada(TipoJornadaFaltaJustificacionFaltaProfesoradoV1.VARIOS_DIAS_PRIMERO_PARCIAL))));
    }

    @Test
    void necesitaFechaFin_sinTipoDeJornada_devuelveFalse() {
        JustificacionFaltaProfesoradoV1 expediente = conTipoJornada(null);

        assertFalse(JustificacionFaltaProfesoradoV1Util.necesitaFechaFin(expediente));
    }

    /* ------------------------------------------------------------------ */
    /* necesitaHoraInicio                                                 */
    /* ------------------------------------------------------------------ */

    @Test
    void necesitaHoraInicio_clasificaCadaTipoDeJornada() {
        assertAll(
                () -> assertFalse(JustificacionFaltaProfesoradoV1Util.necesitaHoraInicio(
                        conTipoJornada(TipoJornadaFaltaJustificacionFaltaProfesoradoV1.UN_DIA_COMPLETO))),
                () -> assertFalse(JustificacionFaltaProfesoradoV1Util.necesitaHoraInicio(
                        conTipoJornada(TipoJornadaFaltaJustificacionFaltaProfesoradoV1.VARIOS_DIAS_COMPLETOS))),
                () -> assertTrue(JustificacionFaltaProfesoradoV1Util.necesitaHoraInicio(
                        conTipoJornada(TipoJornadaFaltaJustificacionFaltaProfesoradoV1.UNAS_HORAS_UN_DIA))),
                () -> assertTrue(JustificacionFaltaProfesoradoV1Util.necesitaHoraInicio(
                        conTipoJornada(TipoJornadaFaltaJustificacionFaltaProfesoradoV1.VARIOS_DIAS_PRIMERO_PARCIAL))));
    }

    @Test
    void necesitaHoraInicio_sinTipoDeJornada_devuelveFalse() {
        JustificacionFaltaProfesoradoV1 expediente = conTipoJornada(null);

        assertFalse(JustificacionFaltaProfesoradoV1Util.necesitaHoraInicio(expediente));
    }

    /* ------------------------------------------------------------------ */
    /* necesitaHoraFin                                                    */
    /* ------------------------------------------------------------------ */

    @Test
    void necesitaHoraFin_clasificaCadaTipoDeJornada() {
        assertAll(
                () -> assertFalse(JustificacionFaltaProfesoradoV1Util.necesitaHoraFin(
                        conTipoJornada(TipoJornadaFaltaJustificacionFaltaProfesoradoV1.UN_DIA_COMPLETO))),
                () -> assertFalse(JustificacionFaltaProfesoradoV1Util.necesitaHoraFin(
                        conTipoJornada(TipoJornadaFaltaJustificacionFaltaProfesoradoV1.VARIOS_DIAS_COMPLETOS))),
                () -> assertTrue(JustificacionFaltaProfesoradoV1Util.necesitaHoraFin(
                        conTipoJornada(TipoJornadaFaltaJustificacionFaltaProfesoradoV1.UNAS_HORAS_UN_DIA))),
                () -> assertFalse(JustificacionFaltaProfesoradoV1Util.necesitaHoraFin(
                        conTipoJornada(TipoJornadaFaltaJustificacionFaltaProfesoradoV1.VARIOS_DIAS_PRIMERO_PARCIAL))));
    }

    @Test
    void necesitaHoraFin_sinTipoDeJornada_devuelveFalse() {
        JustificacionFaltaProfesoradoV1 expediente = conTipoJornada(null);

        assertFalse(JustificacionFaltaProfesoradoV1Util.necesitaHoraFin(expediente));
    }

    /* ------------------------------------------------------------------ */
    /* exigeSerElCreador                                                  */
    /* ------------------------------------------------------------------ */

    private static User usuario(long id) {
        User usuario = new User();
        usuario.setId(id);
        return usuario;
    }

    private static JustificacionFaltaProfesoradoV1 registradoPor(User usuarioRegistrador) {
        JustificacionFaltaProfesoradoV1 expediente = new JustificacionFaltaProfesoradoV1();
        expediente.setUsuarioRegistrador(usuarioRegistrador);
        return expediente;
    }

    /** Ejecuta la comprobación con {@code autenticado} como usuario de la sesión y con I18n devolviendo la clave. */
    private static void conUsuarioAutenticado(User autenticado, Executable comprobacion) throws Throwable {
        try (MockedStatic<SecurityUtil> securityUtil = Mockito.mockStatic(SecurityUtil.class);
             MockedStatic<I18n> i18n = Mockito.mockStatic(I18n.class)) {
            securityUtil.when(SecurityUtil::getUser).thenReturn(autenticado);
            i18n.when(() -> I18n.get(any(String.class))).thenAnswer(invocation -> invocation.getArgument(0));
            comprobacion.execute();
        }
    }

    private static String mensajeDe(BusinessException excepcion) {
        BusinessMessages mensajes = excepcion.getBusinessMessages();
        assertEquals(1, mensajes.size());
        return mensajes.get(0).getMessage();
    }

    @Test
    void exigeSerElCreador_usuarioAutenticadoEsElRegistrador_noLanza() throws Throwable {
        // Instancias de User distintas con el mismo id: la guarda compara ids, no referencias.
        JustificacionFaltaProfesoradoV1 expediente = registradoPor(usuario(7L));

        conUsuarioAutenticado(usuario(7L), () ->
                assertDoesNotThrow(() -> JustificacionFaltaProfesoradoV1Util.exigeSerElCreador(expediente, MENSAJE_PRESENTAR)));
    }

    @Test
    void exigeSerElCreador_usuarioAutenticadoDistintoDelRegistrador_lanzaConElMensajeDeCadaLlamante() throws Throwable {
        JustificacionFaltaProfesoradoV1 expediente = registradoPor(usuario(7L));

        conUsuarioAutenticado(usuario(8L), () -> assertAll(
                () -> assertEquals(MENSAJE_PRESENTAR, mensajeDe(assertThrows(BusinessException.class,
                        () -> JustificacionFaltaProfesoradoV1Util.exigeSerElCreador(expediente, MENSAJE_PRESENTAR)))),
                () -> assertEquals(MENSAJE_BORRAR, mensajeDe(assertThrows(BusinessException.class,
                        () -> JustificacionFaltaProfesoradoV1Util.exigeSerElCreador(expediente, MENSAJE_BORRAR))))));
    }

    @Test
    void exigeSerElCreador_expedienteSinUsuarioRegistrador_lanzaIllegalState() throws Throwable {
        // Lo fija el Tramitador al dar de alta: que falte no es un error que el usuario pueda corregir.
        JustificacionFaltaProfesoradoV1 expediente = registradoPor(null);

        conUsuarioAutenticado(usuario(7L), () -> assertThrows(IllegalStateException.class,
                () -> JustificacionFaltaProfesoradoV1Util.exigeSerElCreador(expediente, MENSAJE_PRESENTAR)));
    }

    @Test
    void exigeSerElCreador_sinUsuarioAutenticado_lanzaNullPointer() throws Throwable {
        JustificacionFaltaProfesoradoV1 expediente = registradoPor(usuario(7L));

        conUsuarioAutenticado(null, () -> assertThrows(NullPointerException.class,
                () -> JustificacionFaltaProfesoradoV1Util.exigeSerElCreador(expediente, MENSAJE_PRESENTAR)));
    }

}
