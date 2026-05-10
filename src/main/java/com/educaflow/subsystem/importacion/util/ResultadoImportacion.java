package com.educaflow.subsystem.importacion.util;

import com.educaflow.subsystem.common.db.Centro;

import java.time.LocalDate;
import java.util.List;

public record ResultadoImportacion(
        String resumen,
        List<MensajeImportacion> mensajes,
        Centro centro,
        Integer curso,
        LocalDate fechaExportacion
) {
}