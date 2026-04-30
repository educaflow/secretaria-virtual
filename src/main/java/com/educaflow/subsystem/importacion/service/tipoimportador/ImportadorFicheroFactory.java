package com.educaflow.subsystem.importacion.service.tipoimportador;

import com.educaflow.subsystem.importacion.db.TipoFicheroImportacion;
import com.educaflow.subsystem.importacion.service.tipoimportador.impl.ImportadorProfesoresExternos;
import com.educaflow.subsystem.importacion.service.tipoimportador.impl.ImportadorUsuarioXml;

import java.util.Map;

public class ImportadorFicheroFactory {

    private static final Map<TipoFicheroImportacion, ImportadorFichero> IMPORTADORES = Map.of(
        TipoFicheroImportacion.PROFESORES,       new ImportadorUsuarioXml("docente",  "PROFESOR",  "/data-import/schemas/profesores.xsd"),
        TipoFicheroImportacion.ALUMNOS,          new ImportadorUsuarioXml("alumno",   "ALUMNO",    "/data-import/schemas/alumnos.xsd"),
        TipoFicheroImportacion.FAMILIARES,       new ImportadorUsuarioXml("familiar", "FAMILIAR",  "/data-import/schemas/familiares.xsd"),
        TipoFicheroImportacion.PROFESOR_EXTERNO, new ImportadorProfesoresExternos()
    );

    public static ImportadorFichero getImportadorFichero(TipoFicheroImportacion tipoFichero) {
        ImportadorFichero importador = IMPORTADORES.get(tipoFichero);
        if (importador == null) {
            throw new ImportadorException("Tipo de fichero sin importador: " + tipoFichero);
        }
        return importador;
    }
}