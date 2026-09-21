package com.educaflow.subsystem.expedientes.util;

import com.educaflow.base.infrastructure.pdf.DocumentoPdf;
import com.educaflow.base.infrastructure.pdf.DocumentoPdfFactory;
import com.educaflow.base.infrastructure.pdf.DocumentoPdfUtil;
import com.educaflow.base.util.Convert;
import com.educaflow.subsystem.expedientes.db.Expediente;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Map;

/**
 * Genera el documento PDF de un expediente a partir de su definición XML.
 *
 * <p>Es el destino del {@code getDocumentoPdf(...)} que cada tipo de expediente lleva en el
 * {@code <extra-code-model>} de su {@code domains.xml}. Vive en {@code expedientes} y no en el
 * motor de tramitación a propósito: la entidad generada quedaría acoplada al motor y eso es un
 * ciclo entre los dos subsistemas. Aquí no hay nada de tramitación — ni estados, ni eventos, ni
 * localizador —, solo el expediente y la infraestructura de PDF.
 *
 * @author logongas
 */
public class ExpedienteDocumentoPdfUtil {

    public static DocumentoPdf getDocumentoPdf(Expediente expediente, String documentoPdfFileName) {
        try {
            Class<?> callerClass = expediente.getClass();

            Path pathFileName = Path.of(documentoPdfFileName);

            try (InputStream in = callerClass.getResourceAsStream(documentoPdfFileName)) {
                if (in == null) {
                    throw new IOException("No se encontró el recurso: " + documentoPdfFileName);
                }
                DocumentoPdf documentoPdfVacio = DocumentoPdfFactory.getDocumentoPdf(in.readAllBytes(), pathFileName.getFileName().toString());

                Map<String, Object> contexto = Map.of("self", expediente, "now", java.time.LocalDateTime.now(Convert.defaultZoneId));

                DocumentoPdf documentoPdfRelleno = DocumentoPdfUtil.generate(documentoPdfVacio, contexto);

                return documentoPdfRelleno;

            }
        } catch (IOException e) {
            throw new RuntimeException("Error al cargar el documento PDF: " + documentoPdfFileName, e);
        }
    }

}
