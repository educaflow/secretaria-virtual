package com.educaflow.subsystem.importacion.importador;

import com.educaflow.subsystem.common.db.Centro;

public record ResultadoImportacion(
        int usuariosImportados,
        int numeroErrores,
        String log,
        Centro centro,
        Integer curso
) {}
