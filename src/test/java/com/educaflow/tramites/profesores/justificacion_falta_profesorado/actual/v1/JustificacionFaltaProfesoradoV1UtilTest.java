package com.educaflow.tramites.profesores.justificacion_falta_profesorado.actual.v1;

import com.educaflow.base.util.Convert;
import com.educaflow.subsystem.expedientes.db.JustificacionFaltaProfesoradoV1;
import com.educaflow.subsystem.expedientes.db.TipoJornadaFaltaJustificacionFaltaProfesoradoV1;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JustificacionFaltaProfesoradoV1UtilTest {

    private static final String MENSAJE_FECHAS_INCOMPLETAS =
            "Se han comparado la fecha de fin y la fecha de inicio sin que las dos estén indicadas";
    private static final String MENSAJE_HORAS_INCOMPLETAS =
            "Se han comparado la hora de fin y la hora de inicio sin que las dos estén indicadas";

    private static final LocalTime LAS_NUEVE = LocalTime.of(9, 0);
    private static final LocalTime LAS_ONCE_Y_MEDIA = LocalTime.of(11, 30);

    /* ------------------------------------------------------------------ */
    /* Helpers                                                            */
    /* ------------------------------------------------------------------ */

    private static JustificacionFaltaProfesoradoV1 conTipoJornada(
            TipoJornadaFaltaJustificacionFaltaProfesoradoV1 tipoJornadaFalta) {
        JustificacionFaltaProfesoradoV1 expediente = new JustificacionFaltaProfesoradoV1();
        expediente.setTipoJornadaFalta(tipoJornadaFalta);
        return expediente;
    }

    private static JustificacionFaltaProfesoradoV1 conFechas(LocalDate fechaInicio, LocalDate fechaFin) {
        JustificacionFaltaProfesoradoV1 expediente = new JustificacionFaltaProfesoradoV1();
        expediente.setFechaInicio(fechaInicio);
        expediente.setFechaFin(fechaFin);
        return expediente;
    }

    private static JustificacionFaltaProfesoradoV1 conHoras(LocalTime horaInicio, LocalTime horaFin) {
        JustificacionFaltaProfesoradoV1 expediente = new JustificacionFaltaProfesoradoV1();
        expediente.setHoraInicio(horaInicio);
        expediente.setHoraFin(horaFin);
        return expediente;
    }

    private static LocalDate hoy() {
        return LocalDate.now(Convert.defaultZoneId);
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
    /* tieneTipoJornadaFalta                                              */
    /* ------------------------------------------------------------------ */

    @Test
    void tieneTipoJornadaFalta_conValor_devuelveTrue() {
        JustificacionFaltaProfesoradoV1 expediente =
                conTipoJornada(TipoJornadaFaltaJustificacionFaltaProfesoradoV1.UN_DIA_COMPLETO);

        assertTrue(JustificacionFaltaProfesoradoV1Util.tieneTipoJornadaFalta(expediente));
    }

    @Test
    void tieneTipoJornadaFalta_sinValor_devuelveFalse() {
        JustificacionFaltaProfesoradoV1 expediente = new JustificacionFaltaProfesoradoV1();

        assertFalse(JustificacionFaltaProfesoradoV1Util.tieneTipoJornadaFalta(expediente));
    }

    /* ------------------------------------------------------------------ */
    /* tieneFechaInicio                                                   */
    /* ------------------------------------------------------------------ */

    @Test
    void tieneFechaInicio_conValor_devuelveTrue() {
        JustificacionFaltaProfesoradoV1 expediente = conFechas(hoy(), null);

        assertTrue(JustificacionFaltaProfesoradoV1Util.tieneFechaInicio(expediente));
    }

    @Test
    void tieneFechaInicio_sinValor_devuelveFalse() {
        JustificacionFaltaProfesoradoV1 expediente = new JustificacionFaltaProfesoradoV1();

        assertFalse(JustificacionFaltaProfesoradoV1Util.tieneFechaInicio(expediente));
    }

    /* ------------------------------------------------------------------ */
    /* tieneFechaFin                                                      */
    /* ------------------------------------------------------------------ */

    @Test
    void tieneFechaFin_conValor_devuelveTrue() {
        JustificacionFaltaProfesoradoV1 expediente = conFechas(null, hoy());

        assertTrue(JustificacionFaltaProfesoradoV1Util.tieneFechaFin(expediente));
    }

    @Test
    void tieneFechaFin_sinValor_devuelveFalse() {
        JustificacionFaltaProfesoradoV1 expediente = new JustificacionFaltaProfesoradoV1();

        assertFalse(JustificacionFaltaProfesoradoV1Util.tieneFechaFin(expediente));
    }

    /* ------------------------------------------------------------------ */
    /* tieneHoraInicio                                                    */
    /* ------------------------------------------------------------------ */

    @Test
    void tieneHoraInicio_conValor_devuelveTrue() {
        JustificacionFaltaProfesoradoV1 expediente = conHoras(LAS_NUEVE, null);

        assertTrue(JustificacionFaltaProfesoradoV1Util.tieneHoraInicio(expediente));
    }

    @Test
    void tieneHoraInicio_sinValor_devuelveFalse() {
        JustificacionFaltaProfesoradoV1 expediente = new JustificacionFaltaProfesoradoV1();

        assertFalse(JustificacionFaltaProfesoradoV1Util.tieneHoraInicio(expediente));
    }

    /* ------------------------------------------------------------------ */
    /* tieneHoraFin                                                       */
    /* ------------------------------------------------------------------ */

    @Test
    void tieneHoraFin_conValor_devuelveTrue() {
        JustificacionFaltaProfesoradoV1 expediente = conHoras(null, LAS_NUEVE);

        assertTrue(JustificacionFaltaProfesoradoV1Util.tieneHoraFin(expediente));
    }

    @Test
    void tieneHoraFin_sinValor_devuelveFalse() {
        JustificacionFaltaProfesoradoV1 expediente = new JustificacionFaltaProfesoradoV1();

        assertFalse(JustificacionFaltaProfesoradoV1Util.tieneHoraFin(expediente));
    }

    /* ------------------------------------------------------------------ */
    /* fechaInicioNoAnteriorAHaceUnAnyo                                   */
    /* ------------------------------------------------------------------ */

    @Test
    void fechaInicioNoAnteriorAHaceUnAnyo_justoHaceUnAnyo_devuelveTrue() {
        JustificacionFaltaProfesoradoV1 expediente = conFechas(hoy().minusYears(1), null);

        assertTrue(JustificacionFaltaProfesoradoV1Util.fechaInicioNoAnteriorAHaceUnAnyo(expediente));
    }

    @Test
    void fechaInicioNoAnteriorAHaceUnAnyo_unDiaAntesDeHaceUnAnyo_devuelveFalse() {
        JustificacionFaltaProfesoradoV1 expediente = conFechas(hoy().minusYears(1).minusDays(1), null);

        assertFalse(JustificacionFaltaProfesoradoV1Util.fechaInicioNoAnteriorAHaceUnAnyo(expediente));
    }

    @Test
    void fechaInicioNoAnteriorAHaceUnAnyo_hoy_devuelveTrue() {
        JustificacionFaltaProfesoradoV1 expediente = conFechas(hoy(), null);

        assertTrue(JustificacionFaltaProfesoradoV1Util.fechaInicioNoAnteriorAHaceUnAnyo(expediente));
    }

    @Test
    void fechaInicioNoAnteriorAHaceUnAnyo_sinFecha_devuelveTrue() {
        JustificacionFaltaProfesoradoV1 expediente = conFechas(null, null);

        assertTrue(JustificacionFaltaProfesoradoV1Util.fechaInicioNoAnteriorAHaceUnAnyo(expediente));
    }

    /* ------------------------------------------------------------------ */
    /* fechaInicioNoPosteriorAHoy                                         */
    /* ------------------------------------------------------------------ */

    @Test
    void fechaInicioNoPosteriorAHoy_hoy_devuelveTrue() {
        JustificacionFaltaProfesoradoV1 expediente = conFechas(hoy(), null);

        assertTrue(JustificacionFaltaProfesoradoV1Util.fechaInicioNoPosteriorAHoy(expediente));
    }

    @Test
    void fechaInicioNoPosteriorAHoy_ayer_devuelveTrue() {
        JustificacionFaltaProfesoradoV1 expediente = conFechas(hoy().minusDays(1), null);

        assertTrue(JustificacionFaltaProfesoradoV1Util.fechaInicioNoPosteriorAHoy(expediente));
    }

    @Test
    void fechaInicioNoPosteriorAHoy_manyana_devuelveFalse() {
        JustificacionFaltaProfesoradoV1 expediente = conFechas(hoy().plusDays(1), null);

        assertFalse(JustificacionFaltaProfesoradoV1Util.fechaInicioNoPosteriorAHoy(expediente));
    }

    @Test
    void fechaInicioNoPosteriorAHoy_sinFecha_devuelveTrue() {
        JustificacionFaltaProfesoradoV1 expediente = conFechas(null, null);

        assertTrue(JustificacionFaltaProfesoradoV1Util.fechaInicioNoPosteriorAHoy(expediente));
    }

    /* ------------------------------------------------------------------ */
    /* fechaFinPosteriorAFechaInicio                                      */
    /* ------------------------------------------------------------------ */

    @Test
    void fechaFinPosteriorAFechaInicio_finDespues_devuelveTrue() {
        LocalDate hoy = hoy();
        JustificacionFaltaProfesoradoV1 expediente = conFechas(hoy.minusDays(3), hoy.minusDays(1));

        assertTrue(JustificacionFaltaProfesoradoV1Util.fechaFinPosteriorAFechaInicio(expediente));
    }

    @Test
    void fechaFinPosteriorAFechaInicio_mismoDia_devuelveFalse() {
        LocalDate mismoDia = hoy().minusDays(1);
        JustificacionFaltaProfesoradoV1 expediente = conFechas(mismoDia, mismoDia);

        assertFalse(JustificacionFaltaProfesoradoV1Util.fechaFinPosteriorAFechaInicio(expediente));
    }

    @Test
    void fechaFinPosteriorAFechaInicio_finAntes_devuelveFalse() {
        LocalDate hoy = hoy();
        JustificacionFaltaProfesoradoV1 expediente = conFechas(hoy.minusDays(1), hoy.minusDays(3));

        assertFalse(JustificacionFaltaProfesoradoV1Util.fechaFinPosteriorAFechaInicio(expediente));
    }

    @Test
    void fechaFinPosteriorAFechaInicio_sinFechaFin_lanzaIllegalState() {
        JustificacionFaltaProfesoradoV1 expediente = conFechas(hoy(), null);

        IllegalStateException excepcion = assertThrows(IllegalStateException.class,
                () -> JustificacionFaltaProfesoradoV1Util.fechaFinPosteriorAFechaInicio(expediente));

        assertEquals(MENSAJE_FECHAS_INCOMPLETAS, excepcion.getMessage());
    }

    @Test
    void fechaFinPosteriorAFechaInicio_sinFechaInicio_lanzaIllegalState() {
        JustificacionFaltaProfesoradoV1 expediente = conFechas(null, hoy());

        IllegalStateException excepcion = assertThrows(IllegalStateException.class,
                () -> JustificacionFaltaProfesoradoV1Util.fechaFinPosteriorAFechaInicio(expediente));

        assertEquals(MENSAJE_FECHAS_INCOMPLETAS, excepcion.getMessage());
    }

    /* ------------------------------------------------------------------ */
    /* fechaFinNoPosteriorAHoy                                            */
    /* ------------------------------------------------------------------ */

    @Test
    void fechaFinNoPosteriorAHoy_hoy_devuelveTrue() {
        JustificacionFaltaProfesoradoV1 expediente = conFechas(null, hoy());

        assertTrue(JustificacionFaltaProfesoradoV1Util.fechaFinNoPosteriorAHoy(expediente));
    }

    @Test
    void fechaFinNoPosteriorAHoy_manyana_devuelveFalse() {
        JustificacionFaltaProfesoradoV1 expediente = conFechas(null, hoy().plusDays(1));

        assertFalse(JustificacionFaltaProfesoradoV1Util.fechaFinNoPosteriorAHoy(expediente));
    }

    @Test
    void fechaFinNoPosteriorAHoy_sinFecha_devuelveTrue() {
        JustificacionFaltaProfesoradoV1 expediente = conFechas(null, null);

        assertTrue(JustificacionFaltaProfesoradoV1Util.fechaFinNoPosteriorAHoy(expediente));
    }

    /* ------------------------------------------------------------------ */
    /* horaFinPosteriorAHoraInicio                                        */
    /* ------------------------------------------------------------------ */

    @Test
    void horaFinPosteriorAHoraInicio_finDespues_devuelveTrue() {
        JustificacionFaltaProfesoradoV1 expediente = conHoras(LAS_NUEVE, LAS_ONCE_Y_MEDIA);

        assertTrue(JustificacionFaltaProfesoradoV1Util.horaFinPosteriorAHoraInicio(expediente));
    }

    @Test
    void horaFinPosteriorAHoraInicio_mismaHora_devuelveFalse() {
        JustificacionFaltaProfesoradoV1 expediente = conHoras(LAS_NUEVE, LAS_NUEVE);

        assertFalse(JustificacionFaltaProfesoradoV1Util.horaFinPosteriorAHoraInicio(expediente));
    }

    @Test
    void horaFinPosteriorAHoraInicio_finAntes_devuelveFalse() {
        JustificacionFaltaProfesoradoV1 expediente = conHoras(LAS_ONCE_Y_MEDIA, LAS_NUEVE);

        assertFalse(JustificacionFaltaProfesoradoV1Util.horaFinPosteriorAHoraInicio(expediente));
    }

    @Test
    void horaFinPosteriorAHoraInicio_sinHoraFin_lanzaIllegalState() {
        JustificacionFaltaProfesoradoV1 expediente = conHoras(LAS_NUEVE, null);

        IllegalStateException excepcion = assertThrows(IllegalStateException.class,
                () -> JustificacionFaltaProfesoradoV1Util.horaFinPosteriorAHoraInicio(expediente));

        assertEquals(MENSAJE_HORAS_INCOMPLETAS, excepcion.getMessage());
    }

    @Test
    void horaFinPosteriorAHoraInicio_sinHoraInicio_lanzaIllegalState() {
        JustificacionFaltaProfesoradoV1 expediente = conHoras(null, LAS_ONCE_Y_MEDIA);

        IllegalStateException excepcion = assertThrows(IllegalStateException.class,
                () -> JustificacionFaltaProfesoradoV1Util.horaFinPosteriorAHoraInicio(expediente));

        assertEquals(MENSAJE_HORAS_INCOMPLETAS, excepcion.getMessage());
    }

}
