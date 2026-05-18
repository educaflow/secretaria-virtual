package com.educaflow.subsystem.importacion.importador;

import com.educaflow.subsystem.common.db.Centro;

public record ResultadoImportacion(
        int creados,
        int ignorados,
        int errores,
        String log,
        Centro centro,
        Integer curso
) {}
