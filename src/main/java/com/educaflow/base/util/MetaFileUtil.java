package com.educaflow.base.util;

import com.axelor.inject.Beans;
import com.axelor.meta.db.MetaFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;

public class MetaFileUtil {
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

}
