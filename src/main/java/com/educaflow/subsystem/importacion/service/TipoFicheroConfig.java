package com.educaflow.subsystem.importacion.service;

import com.educaflow.subsystem.importacion.db.TipoFicheroImportacion;

import java.util.Map;

public class TipoFicheroConfig {

    public record Rutas(String configPath, String schemaPath) {}

    private static final Map<TipoFicheroImportacion, Rutas> CONFIG = Map.of(
        TipoFicheroImportacion.PROFESORES, new Rutas("/data-import/profesores-config.xml", "/data-import/schemas/profesores.xsd"),
        TipoFicheroImportacion.ALUMNOS,    new Rutas("/data-import/alumnos-config.xml",    "/data-import/schemas/alumnos.xsd"),
        TipoFicheroImportacion.FAMILIARES, new Rutas("/data-import/familiares-config.xml", "/data-import/schemas/familiares.xsd"),
        TipoFicheroImportacion.PRUEBA, new Rutas(null, null)
    );

    public static Rutas getRutas(TipoFicheroImportacion tipo) {
        return CONFIG.get(tipo);
    }

}