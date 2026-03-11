package com.educaflow.base.util;

import com.axelor.data.ImportTask;
import com.axelor.inject.Beans;
import com.axelor.meta.MetaFiles;
import com.axelor.meta.db.MetaFile;
import com.axelor.meta.db.repo.MetaFileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Map;

public class MetaFileUtil {

    private static final Logger logger = LoggerFactory.getLogger(ImportTask.class);

    public static byte[] downloadContent(MetaFile metaFile) {
        try {
            Path filePath= Beans.get(com.axelor.meta.MetaFiles.class).getPath(metaFile);
            byte[] content = Files.readAllBytes(filePath);

            return content;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

    }

    public static void uploadContent(MetaFile metaFile, byte[] content) {
        try {
            InputStream inputStream = new ByteArrayInputStream(content);


            Beans.get(com.axelor.meta.MetaFiles.class).upload(inputStream, metaFile);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

    }

    public static <T extends MetaFile> T cloneMetaFile(T metaFile) {
        if (metaFile == null) {
            return null;
        }
        byte[] bytes = MetaFileUtil.downloadContent(metaFile);

        T nuevoMetaFile = createMetaFileInstance((Class<T>)metaFile.getClass());
        nuevoMetaFile.setFileName(metaFile.getFileName());
        nuevoMetaFile.setFileType(metaFile.getFileType());

        MetaFileUtil.uploadContent(nuevoMetaFile, bytes);

        return nuevoMetaFile;
    }

    public static String sha256(MetaFile metaFile) {
        try {
            byte[] content=downloadContent(metaFile);

            return CryptoUtil.sha256(content);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

    }

    public static <T extends MetaFile> T createMetaFileInstance(Class<T> clazz) {
        try {
            T metaFile = clazz.getDeclaredConstructor().newInstance();

            return metaFile;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Método para obtener el MetaFile de forma segura.
     * Axelor a veces envía el objeto y otras un Map con el ID.
     */
    public static MetaFile getMetaFile(Object obj) {
        if (obj == null) return null;

        if (obj instanceof MetaFile) {
            return (MetaFile) obj;
        }

        if (obj instanceof Map) {
            try {
                Object idObj = ((Map<?, ?>) obj).get("id");
                if (idObj != null) {
                    Long id = Long.valueOf(idObj.toString());
                    return Beans.get(MetaFileRepository.class).find(id);
                }
            } catch (Exception e) {
                logger.error("No se pudo recuperar el ID del MetaFile desde el mapa del contexto", e);
            }
        }
        return null;
    }

    public static void deleteSafe(MetaFile file) {
        if (file == null || file.getId() == null) return;

        try {
            logger.info("Eliminando archivo procesado: {}", file.getFileName());

            // 1. Obtenemos el repositorio para limpiar el proxy
            MetaFileRepository repo = Beans.get(MetaFileRepository.class);

            // 2. Buscamos la entidad real por ID para que Hibernate no se queje
            MetaFile entityToDelete = repo.find(file.getId());

            if (entityToDelete != null) {
                // 3. Ahora sí, usamos la API de MetaFiles con una entidad real
                Beans.get(MetaFiles.class).delete(entityToDelete);
            }
        } catch (Exception ex) {
            logger.error("No se pudo eliminar el archivo MetaFile: {}", ex.getMessage());
        }

    }

}
