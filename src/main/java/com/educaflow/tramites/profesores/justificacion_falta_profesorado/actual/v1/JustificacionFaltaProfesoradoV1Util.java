package com.educaflow.tramites.profesores.justificacion_falta_profesorado.actual.v1;

import com.educaflow.subsystem.expedientes.db.JustificacionFaltaProfesoradoV1;

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

}
