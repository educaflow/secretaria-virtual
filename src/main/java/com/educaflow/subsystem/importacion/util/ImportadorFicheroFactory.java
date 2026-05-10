package com.educaflow.subsystem.importacion.util;

import com.axelor.inject.Beans;
import com.educaflow.subsystem.importacion.db.TipoFicheroImportacion;
import com.educaflow.subsystem.importacion.util.impl.ImportadorUsuarioCsv;
import com.educaflow.subsystem.importacion.util.impl.ImportadorUsuarioXml;
import com.google.inject.Injector;

public class ImportadorFicheroFactory {

    public static ImportadorFichero getImportadorFichero(TipoFicheroImportacion tipoFichero, byte[] contenido) {
        Injector injector = Beans.get(Injector.class);
        ImportadorFichero importador = switch (tipoFichero) {
            case PROFESOR, ALUMNO, FAMILIAR -> new ImportadorUsuarioXml(tipoFichero, contenido);
            case PROFESOR_EXTERNO        -> new ImportadorUsuarioCsv(tipoFichero, contenido);
        };
        injector.injectMembers(importador);
        return importador;
    }
}