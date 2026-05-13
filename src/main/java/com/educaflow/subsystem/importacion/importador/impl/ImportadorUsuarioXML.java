package com.educaflow.subsystem.importacion.importador.impl;

import com.axelor.meta.db.MetaFile;
import com.educaflow.subsystem.importacion.db.TipoFicheroImportacion;
import com.educaflow.subsystem.importacion.exception.ImportadorException;
import com.educaflow.subsystem.importacion.importador.ImportadorFichero;
import com.educaflow.subsystem.importacion.importador.ResultadoImportacion;

public class ImportadorUsuarioXML implements ImportadorFichero {

    private final MetaFile fichero;
    private final TipoFicheroImportacion tipoFichero;

    public ImportadorUsuarioXML(MetaFile fichero, TipoFicheroImportacion tipoFichero) {
        this.fichero = fichero;
        this.tipoFichero = tipoFichero;
    }

    @Override
    public ResultadoImportacion importar() throws ImportadorException {
        throw new ImportadorException("@TODO: Importación no implementada todavía");
    }
}
