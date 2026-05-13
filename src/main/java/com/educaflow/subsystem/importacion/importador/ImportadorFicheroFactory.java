package com.educaflow.subsystem.importacion.importador;

import com.axelor.meta.db.MetaFile;
import com.educaflow.subsystem.importacion.db.TipoFicheroImportacion;
import com.educaflow.subsystem.importacion.importador.impl.ImportadorUsuarioCSV;
import com.educaflow.subsystem.importacion.importador.impl.ImportadorUsuarioXML;

public final class ImportadorFicheroFactory {

    private ImportadorFicheroFactory() {}

    public static ImportadorFichero create(TipoFicheroImportacion tipoFichero, MetaFile fichero) {
        if (tipoFichero == TipoFicheroImportacion.PROFESOR
                || tipoFichero == TipoFicheroImportacion.ALUMNO
                || tipoFichero == TipoFicheroImportacion.FAMILIAR) {
            return new ImportadorUsuarioXML(fichero, tipoFichero);
        } else if (tipoFichero == TipoFicheroImportacion.PROFESOR_EXTERNO) {
            return new ImportadorUsuarioCSV(fichero, tipoFichero);
        } else {
            throw new IllegalArgumentException("Tipo de fichero no soportado: " + tipoFichero);
        }
    }
}
