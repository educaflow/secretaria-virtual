package com.educaflow.tramites.profesores.justificacion_falta_profesorado.actual.v1;

import com.educaflow.base.util.Convert;
import com.educaflow.subsystem.expedientes.db.JustificacionFaltaProfesoradoV1;

import java.time.LocalDate;
import java.time.LocalTime;

public final class JustificacionFaltaProfesoradoV1Util {

    private JustificacionFaltaProfesoradoV1Util() {
    }

    /* ------------------------------------------------------------------ */
    /* Campos que pide cada tipo de jornada faltada                       */
    /* ------------------------------------------------------------------ */

    // Sin default a propósito: así un ítem nuevo del enum sin clasificar no compila, en vez de quedarse en false.
    public static boolean necesitaFechaFin(JustificacionFaltaProfesoradoV1 expediente) {
        return switch (expediente.getTipoJornadaFalta()) {
            case null -> false;
            case VARIOS_DIAS_COMPLETOS, VARIOS_DIAS_PRIMERO_PARCIAL -> true;
            case UN_DIA_COMPLETO, UNAS_HORAS_UN_DIA -> false;
        };
    }

    public static boolean necesitaHoraInicio(JustificacionFaltaProfesoradoV1 expediente) {
        return switch (expediente.getTipoJornadaFalta()) {
            case null -> false;
            case UNAS_HORAS_UN_DIA, VARIOS_DIAS_PRIMERO_PARCIAL -> true;
            case UN_DIA_COMPLETO, VARIOS_DIAS_COMPLETOS -> false;
        };
    }

    public static boolean necesitaHoraFin(JustificacionFaltaProfesoradoV1 expediente) {
        return switch (expediente.getTipoJornadaFalta()) {
            case null -> false;
            case UNAS_HORAS_UN_DIA -> true;
            case UN_DIA_COMPLETO, VARIOS_DIAS_COMPLETOS, VARIOS_DIAS_PRIMERO_PARCIAL -> false;
        };
    }

    /* ------------------------------------------------------------------ */
    /* Tipo de jornada faltada                                            */
    /* ------------------------------------------------------------------ */

    public static boolean tieneTipoJornadaFalta(JustificacionFaltaProfesoradoV1 expediente) {
        return expediente.getTipoJornadaFalta() != null;
    }

    /* ------------------------------------------------------------------ */
    /* Fechas                                                             */
    /* ------------------------------------------------------------------ */

    public static boolean tieneFechaInicio(JustificacionFaltaProfesoradoV1 expediente) {
        return expediente.getFechaInicio() != null;
    }

    public static boolean fechaInicioNoAnteriorAHaceUnAnyo(JustificacionFaltaProfesoradoV1 expediente) {
        LocalDate fechaInicio = expediente.getFechaInicio();

        return (fechaInicio == null)
                || (fechaInicio.isBefore(LocalDate.now(Convert.defaultZoneId).minusYears(1)) == false);
    }

    public static boolean fechaInicioNoPosteriorAHoy(JustificacionFaltaProfesoradoV1 expediente) {
        LocalDate fechaInicio = expediente.getFechaInicio();

        return (fechaInicio == null)
                || (fechaInicio.isAfter(LocalDate.now(Convert.defaultZoneId)) == false);
    }

    public static boolean tieneFechaFin(JustificacionFaltaProfesoradoV1 expediente) {
        return expediente.getFechaFin() != null;
    }

    public static boolean fechaFinPosteriorAFechaInicio(JustificacionFaltaProfesoradoV1 expediente) {
        LocalDate fechaFin = expediente.getFechaFin();
        LocalDate fechaInicio = expediente.getFechaInicio();

        if (fechaFin == null || fechaInicio == null) {
            throw new IllegalStateException("Se han comparado la fecha de fin y la fecha de inicio sin que las dos estén indicadas");
        }

        return fechaFin.isAfter(fechaInicio);
    }

    public static boolean fechaFinNoPosteriorAHoy(JustificacionFaltaProfesoradoV1 expediente) {
        LocalDate fechaFin = expediente.getFechaFin();

        return (fechaFin == null)
                || (fechaFin.isAfter(LocalDate.now(Convert.defaultZoneId)) == false);
    }

    /* ------------------------------------------------------------------ */
    /* Horas                                                              */
    /* ------------------------------------------------------------------ */

    public static boolean tieneHoraInicio(JustificacionFaltaProfesoradoV1 expediente) {
        return expediente.getHoraInicio() != null;
    }

    public static boolean tieneHoraFin(JustificacionFaltaProfesoradoV1 expediente) {
        return expediente.getHoraFin() != null;
    }

    public static boolean horaFinPosteriorAHoraInicio(JustificacionFaltaProfesoradoV1 expediente) {
        LocalTime horaFin = expediente.getHoraFin();
        LocalTime horaInicio = expediente.getHoraInicio();

        if (horaFin == null || horaInicio == null) {
            throw new IllegalStateException("Se han comparado la hora de fin y la hora de inicio sin que las dos estén indicadas");
        }

        return horaFin.isAfter(horaInicio);
    }

}
