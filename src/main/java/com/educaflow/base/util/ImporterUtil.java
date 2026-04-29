package com.educaflow.base.util;

import com.educaflow.base.infrastructure.importer.ImportConfigException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class ImporterUtil {

    private static final Logger logger = LoggerFactory.getLogger(ImporterUtil.class);

    private ImporterUtil() {}

    public static Path crearFicheroTemporalFromResource(String resourcePath) {
        try (InputStream is = ImporterUtil.class.getResourceAsStream(resourcePath)) {
            if (is == null) throw new ImportConfigException("Fichero de configuración no encontrado: " + resourcePath);
            Path temp = Files.createTempFile("import-config-", ".xml");
            Files.copy(is, temp, StandardCopyOption.REPLACE_EXISTING);
            return temp;
        } catch (IOException e) {
            throw new ImportConfigException("Error creando fichero temporal de configuración: " + resourcePath, e);
        }
    }

    public static void borrarArchivoTemporal(Path path) {
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
