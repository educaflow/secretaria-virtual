package com.educaflow.subsystem.importer.services.impl;

import com.axelor.data.ImportTask;
import com.axelor.data.Importer;
import com.axelor.data.xml.XMLImporter;
import com.educaflow.base.util.MetaFileUtil;
import com.educaflow.base.util.XmlUtil;
import com.educaflow.subsystem.importer.db.ViewDataImport;
import com.educaflow.subsystem.importer.services.FileImporter;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FileImporterImpl implements FileImporter {

    private final Logger logger = LoggerFactory.getLogger(FileImporter.class);
    private final String IMPORT_LOGGER_NAME = "importLogger";
    private final String DATA_FILE_NAME = "dataFile";

    @Inject
    private Provider<ImportTask> importTaskProvider;

    @Override
    public List<String> importFile(ViewDataImport viewDataImport) {

        List<String> importLog = new ArrayList<>();
        Path tempConfigFile = null;

        try {
            byte[] dataFileContent = MetaFileUtil.downloadContent(viewDataImport.getDataFile());

            if (viewDataImport.getValidationSchemaPath() != null) {
                if (!ejecutarValidacion(dataFileContent, viewDataImport, importLog)) {
                    return importLog;
                }
            }

            tempConfigFile = crearFicheroTemporalFromResource(viewDataImport.getConfigFilePath());

            ejecutarImportacion(tempConfigFile, dataFileContent, importLog);
        } catch (Exception e) {
            logger.error("Error durante la importación del archivo", e);
            throw new RuntimeException(e);
        } finally {
            borrarArchivoTemporal(tempConfigFile);
        }

        return importLog;
    }

    private boolean ejecutarValidacion(byte[] content, ViewDataImport view, List<String> log) {
        XmlUtil.ValidationResult result = validarConSchema(content, view.getValidationSchemaPath());
        if (!result.success()) {
            log.add("Error de validación XML: " + result.message());
            return false;
        }
        return true;
    }

    private XmlUtil.ValidationResult validarConSchema(byte[] dataFileContent, String validationSchemaPath) {
        try (InputStream validationSchema = this.getClass().getResourceAsStream(validationSchemaPath); InputStream archivoImportacion = new ByteArrayInputStream(dataFileContent)) {
            if (validationSchema == null) {
                return new XmlUtil.ValidationResult(false, "No se pudo cargar el esquema de validación desde la ruta: " + validationSchemaPath);
            }
            return XmlUtil.validarConSchema(archivoImportacion, validationSchema);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void ejecutarImportacion(Path configPath, byte[] data, List<String> log) {
        Importer importer = new XMLImporter(configPath.toAbsolutePath().toString());
        importer.setContext(Map.of(IMPORT_LOGGER_NAME, log));

        importer.addListener(new ListenerImpl(log));

        ImportTask task = importTaskProvider.get();
        task.input(DATA_FILE_NAME, new ByteArrayInputStream(data));

        importer.run(task);
    }

    private Path crearFicheroTemporalFromResource(String resourcePath) throws IOException {
        try (InputStream is = this.getClass().getResourceAsStream(resourcePath)) {
            if (is == null) throw new FileNotFoundException("No se encontró: " + resourcePath);

            Path temp = Files.createTempFile("import-config-", ".xml");
            Files.copy(is, temp, StandardCopyOption.REPLACE_EXISTING);
            return temp;
        }
    }

    private void borrarArchivoTemporal(Path path) {
        if (path != null) {
            try {
                Files.deleteIfExists(path);
                logger.info("Temporal eliminado: {}", path);
            } catch (IOException e) {
                logger.warn("No se pudo eliminar el temporal: {}", path);
            }
        }
    }

}
