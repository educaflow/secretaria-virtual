package com.educaflow.tramites.profesores.justificacion_falta_profesorado.actual.v1;

import com.educaflow.subsystem.expedientes.db.JustificacionFaltaProfesoradoV1;
import com.educaflow.subsystem.expedientes.db.TipoJornadaFaltaJustificacionFaltaProfesoradoV1;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JustificacionFaltaProfesoradoV1UtilTest {

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

}
