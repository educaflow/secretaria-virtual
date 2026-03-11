package com.educaflow.base.infrastructure.metafile;

import com.axelor.meta.db.MetaFile;
import com.educaflow.base.infrastructure.pdf.DocumentoPdf;
import com.educaflow.base.infrastructure.pdf.DocumentoPdfFactory;
import com.educaflow.base.util.MetaFileUtil;

public class MetaFileHelper {

    public static final String PDF_MIME_TYPE = "application/pdf";

    public static <T extends MetaFile> T createMetaFile(DocumentoPdf documentoPdf, Class<T> clazz) {
        T metaFile = MetaFileUtil.createMetaFileInstance(clazz);
        metaFile.setFileName(documentoPdf.getFileName());
        metaFile.setFileType(PDF_MIME_TYPE);

        byte[] bytes = documentoPdf.getDatos();
        MetaFileUtil.uploadContent(metaFile, bytes);

        return metaFile;
    }


    public static MetaFile createMetaFile(DocumentoPdf documentoPdf) {
        return createMetaFile(documentoPdf, MetaFile.class);
    }



    public static DocumentoPdf getDocumentoPdf(MetaFile metaFile) {
        if (metaFile == null) {
            return null;
        }

        if (isPdf(metaFile)==false) {
            throw new RuntimeException("El MetaFile no es de tipo PDF");
        }

        byte[] bytes = MetaFileUtil.downloadContent(metaFile);
        DocumentoPdf documentoPdf= DocumentoPdfFactory.getDocumentoPdf(bytes, metaFile.getFileName());

        return documentoPdf;
    }


    public static boolean isPdf(MetaFile metaFile) {
        if (metaFile == null) {
            return false;
        }
        String fileType = metaFile.getFileType();

        if (fileType == null) {
            throw new RuntimeException("El MetaFile no tiene fileType definido");
        }

        return PDF_MIME_TYPE.equalsIgnoreCase(fileType);
    }




}
