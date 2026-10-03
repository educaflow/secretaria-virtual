package com.educaflow.base.util;

import com.axelor.inject.Beans;
import com.axelor.meta.MetaFiles;
import com.axelor.meta.db.MetaFile;
import com.axelor.meta.db.repo.MetaFileRepository;
import com.axelor.script.ScriptAllowed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@ScriptAllowed
public class MetaFileUtil {

    private static final Logger logger = LoggerFactory.getLogger(MetaFileUtil.class);

    public static byte[] downloadContent(MetaFile metaFile) {
        try {
            Path filePath= Beans.get(com.axelor.meta.MetaFiles.class).getPath(metaFile);
            byte[] content = Files.readAllBytes(filePath);

            return content;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

    }

    /** Lee solo los primeros {@code maxBytes} del fichero, Se usa para saber el tipo de los ficheros */
    public static byte[] downloadHeader(MetaFile metaFile, int maxBytes) {
        try (InputStream inputStream = Files.newInputStream(Beans.get(MetaFiles.class).getPath(metaFile))) {
            return inputStream.readNBytes(maxBytes);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static MetaFile uploadContent(MetaFile metaFile, byte[] content) {
        try {
            InputStream inputStream = new ByteArrayInputStream(content);


            return Beans.get(com.axelor.meta.MetaFiles.class).upload(inputStream, metaFile);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

    }

    public static MetaFile cloneMetaFile(MetaFile metaFile) {
        if (metaFile == null) {
            return null;
        }
        try {
            byte[] bytes = MetaFileUtil.downloadContent(metaFile);
            InputStream inputStream = new ByteArrayInputStream(bytes);
            // upload(InputStream, String) crea un MetaFile nuevo con filePath correcto.
            // No usar upload(InputStream, MetaFile) con un MetaFile sin filePath: falla con NPE.
            return Beans.get(MetaFiles.class).upload(inputStream, metaFile.getFileName());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static String sha256(MetaFile metaFile) {
        try {
            byte[] content=downloadContent(metaFile);

            return CryptoUtil.sha256(content);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

    }

    public static MetaFile createMetaFileInstance() {
        try {
            MetaFile metaFile = MetaFile.class.getDeclaredConstructor().newInstance();

            return metaFile;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static void delete(MetaFile file) {
        if (file == null || file.getId() == null) return;

        try {
            logger.info("Eliminando archivo procesado id={}", file.getId());

            // Se vuelve a buscar por id porque MetaFiles.delete falla si recibe un proxy de Hibernate.
            MetaFileRepository repo = Beans.get(MetaFileRepository.class);
            MetaFile entityToDelete = repo.find(file.getId());

            if (entityToDelete != null) {
                Beans.get(MetaFiles.class).delete(entityToDelete);
            }
        } catch (Exception ex) {
            logger.error("No se pudo eliminar el MetaFile id={}", file.getId(), ex);
        }
    }

}
