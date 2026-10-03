package com.educaflow.base.infrastructure.metafile;

import com.axelor.meta.db.MetaFile;
import com.educaflow.base.infrastructure.pdf.DocumentoPdf;
import com.educaflow.base.infrastructure.pdf.DocumentoPdfFactory;
import com.educaflow.base.util.MetaFileUtil;

import java.nio.charset.StandardCharsets;

public class MetaFileHelper {

    public static final String PDF_MIME_TYPE = "application/pdf";
    private static final String CABECERA_PDF = "%PDF-";
    // iText (PdfTokenizer.getHeaderOffset) acepta la cabecera en cualquier punto de los primeros 1024 bytes.
    private static final int BYTES_BUSQUEDA_CABECERA_PDF = 1024;


    public static MetaFile createMetaFile(DocumentoPdf documentoPdf) {
        MetaFile metaFile = MetaFileUtil.createMetaFileInstance();
        metaFile.setFileName(documentoPdf.getFileName());
        metaFile.setFileType(PDF_MIME_TYPE);

        byte[] bytes = documentoPdf.getDatos();
        metaFile=MetaFileUtil.uploadContent(metaFile, bytes);

        return metaFile;
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


    /**
     * Devuelve el MetaFile como PDF: si ya es un PDF tal cual, y si es una imagen que soporta iText, convertida
     * a un PDF de una única página A4 (ver {@link DocumentoPdfFactory#getDocumentoPdfFromImagen}).
     * Si no es ninguna de las dos cosas lanza una excepción.
     */
    public static DocumentoPdf getDocumentoPdfFromImagenOrPdf(MetaFile metaFile) {
        if (metaFile == null) {
            return null;
        }

        if (isPdf(metaFile)) {
            return getDocumentoPdf(metaFile);
        }

        byte[] bytes = MetaFileUtil.downloadContent(metaFile);
        if (DocumentoPdfFactory.isImagenSoportada(bytes)==false) {
            throw new RuntimeException("El MetaFile con id " + metaFile.getId() + " no es ni un PDF ni una imagen soportada");
        }
        DocumentoPdf documentoPdf= DocumentoPdfFactory.getDocumentoPdfFromImagen(bytes, metaFile.getFileName());

        return documentoPdf;
    }


    /** El tipo se decide por la cabecera del contenido: el fileType es el Content-Type que declaró el cliente al subirlo. */
    public static boolean isPdf(MetaFile metaFile) {
        if (metaFile == null) {
            return false;
        }
        byte[] cabecera = MetaFileUtil.downloadHeader(metaFile, BYTES_BUSQUEDA_CABECERA_PDF);

        // ISO-8859-1 mapea cada byte a un carácter, así que buscar en el texto es buscar en los bytes.
        return new String(cabecera, StandardCharsets.ISO_8859_1).contains(CABECERA_PDF);
    }




}
