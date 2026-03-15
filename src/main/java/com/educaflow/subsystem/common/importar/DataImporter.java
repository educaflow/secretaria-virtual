package com.educaflow.subsystem.common.importar;

import com.axelor.data.ImportTask;
import com.axelor.data.xml.XMLImporter;
import com.axelor.meta.CallMethod;
import com.axelor.meta.MetaFiles;
import com.axelor.meta.db.MetaFile;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.educaflow.base.util.MetaFileUtil;
import com.educaflow.base.infrastructure.importer.util.ImportTaskUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;

public class DataImporter {

    private final Logger logger = LoggerFactory.getLogger(DataImporter.class);

    @CallMethod
    public void importarUsuarios(ActionRequest request, ActionResponse response) {
        Object proObj = request.getContext().get("importarProfesores");
        Object aluObj = request.getContext().get("importarAlumnos");

        try {
            boolean procesado = false;

            // 1. Procesar Profesores si existe el fichero
            if (proObj != null) {
                MetaFile file = MetaFileUtil.getMetaFile(proObj);
                if (file != null) {
                    logger.info("Iniciando importación de PROFESORES: {}", file.getFileName());
                    this.ejecutarLote("profesores", "profesores-config.xml", file);
                    response.setValue("importarProfesores", null); // Limpiar campo en la vista
                    procesado = true;
                }
            }

            // 2. Procesar Alumnos si existe el fichero
            if (aluObj != null) {
                MetaFile file = MetaFileUtil.getMetaFile(aluObj);
                if (file != null) {
                    logger.info("Iniciando importación de ALUMNOS: {}", file.getFileName());
                    this.ejecutarLote("alumnos", "alumnos-config.xml", file);
                    response.setValue("importarAlumnos", null); // Limpiar campo en la vista
                    procesado = true;
                }
            }

            if (procesado) {
                response.setReload(true);
                response.setNotify("Proceso de importación finalizado con éxito.");
            } else {
                response.setAlert("No se ha seleccionado ningún archivo para importar.");
            }

        } catch (Exception e) {
            logger.error("Error crítico durante la importación", e);
            response.setError("Error en la importación: " + e.getMessage());
        }
    }

    /**
     * Método interno para ejecutar el motor XMLImporter de Axelor.
     */
    private void ejecutarLote(String inputName, String configFile, MetaFile metaFile) throws Exception {

        File archivoImportacion = MetaFiles.getPath(metaFile).toFile();

        // 2. Localizar la carpeta 'import' en el classpath
        URL importFolderUrl = this.getClass().getResource("/import/");
        if (importFolderUrl == null) {
            throw new RuntimeException("No se encontró la carpeta /import/ en los recursos.");
        }

        // 3. Convertir URL a Path usando NIO (esto limpia el protocolo file:)
        java.nio.file.Path baseDir = java.nio.file.Paths.get(importFolderUrl.toURI());

        // 4. Usar RESOLVE para construir la ruta final
        java.nio.file.Path finalPath = baseDir.resolve(configFile);
        String absolutePath = finalPath.toAbsolutePath().toString();

        // --- LOGS DE DEPURACIÓN ---
        logger.info("Base Directory Path: {}", baseDir);
        logger.info("Ruta final resuelta: {}", absolutePath);
        logger.info("¿El archivo existe físicamente?: {}", java.nio.file.Files.exists(finalPath));
        // --------------------------

        // 5. Instanciar el importador de Axelor
        // Si Files.exists devuelve true, el constructor NO debería fallar
        XMLImporter importer = new XMLImporter(absolutePath);

        logger.info("Configuración cargada");

        ImportTask importTask = ImportTaskUtil.getImportTask();
        importTask.input(inputName, archivoImportacion);

        importer.run(importTask);
    }

}