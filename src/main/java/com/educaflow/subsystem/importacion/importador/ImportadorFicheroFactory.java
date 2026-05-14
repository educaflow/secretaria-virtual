package com.educaflow.subsystem.importacion.importador;

import com.axelor.meta.db.MetaFile;
import com.educaflow.subsystem.importacion.db.TipoFicheroImportacion;
import com.educaflow.subsystem.importacion.importador.impl.ImportadorUsuarioCSV;
import com.educaflow.subsystem.importacion.importador.impl.ImportadorUsuarioXML;

public final class ImportadorFicheroFactory {

    private ImportadorFicheroFactory() {}

    public static ImportadorFichero create(TipoFicheroImportacion tipoFichero, MetaFile fichero) {
        return switch (tipoFichero) {
            case PROFESOR, ALUMNO, FAMILIAR -> new ImportadorUsuarioXML(fichero, tipoFichero);
            case PROFESOR_EXTERNO           -> new ImportadorUsuarioCSV(fichero, tipoFichero);
        };
    }
}
