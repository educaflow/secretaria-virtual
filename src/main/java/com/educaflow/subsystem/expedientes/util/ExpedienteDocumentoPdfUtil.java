package com.educaflow.subsystem.expedientes.util;

import com.axelor.auth.db.User;
import com.educaflow.base.infrastructure.pdf.DocumentoPdf;
import com.educaflow.base.infrastructure.pdf.DocumentoPdfFactory;
import com.educaflow.base.infrastructure.pdf.DocumentoPdfUtil;
import com.educaflow.base.infrastructure.pdfgenerator.Idioma;
import com.educaflow.base.infrastructure.pdfgenerator.PdfGeneratorFactory;
import com.educaflow.base.util.Convert;
import com.educaflow.base.util.SecurityUtil;
import com.educaflow.subsystem.expedientes.db.Expediente;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Map;

/**
 * Obtiene el documento PDF de un expediente, ya relleno con sus datos.
 *
 * <p>Es el destino del {@code getDocumentoPdf(...)} que cada tipo de expediente lleva en el
 * {@code <extra-code-model>} de su {@code domains.xml}.
 *
 * <p>El recurso que trae la constante del enum {@code TipoDocumentoPdf} decide cómo se obtiene:
 * <ul>
 *   <li>{@code .xml}: el XML de definición resuelto por el build; el PDF se <b>genera</b> en runtime
 *       con {@code pdfgenerator} (plano, sin formulario), aplicando los {@code visible} del documento
 *       y evaluando sus expresiones sobre el expediente.</li>
 *   <li>{@code .pdf}: un PDF versionado con formulario; se <b>rellena</b> y aplana evaluando los
 *       nombres de sus campos.</li>
 * </ul>
 *
 * @author logongas
 */
public class ExpedienteDocumentoPdfUtil {

    public static DocumentoPdf getDocumentoPdf(Expediente expediente, String documentoFileName) {
        byte[] recurso = leerRecurso(expediente.getClass(), documentoFileName);
        String nombrePdf = nombrePdf(documentoFileName);
        Map<String, Object> contexto = Map.of("self", expediente, "now", java.time.LocalDateTime.now(Convert.defaultZoneId));

        if (documentoFileName.endsWith(".xml")) {
            byte[] pdf = PdfGeneratorFactory.getPdfGenerator().generate(recurso, contexto, idiomaDelUsuarioAutenticado());
            return DocumentoPdfFactory.getDocumentoPdf(pdf, nombrePdf);
        }

        DocumentoPdf documentoPdfVacio = DocumentoPdfFactory.getDocumentoPdf(recurso, nombrePdf);
        return DocumentoPdfUtil.generate(documentoPdfVacio, contexto);
    }

    private static Idioma idiomaDelUsuarioAutenticado() {
        User usuario = SecurityUtil.getUser();
        return Idioma.deCodigo(usuario == null ? null : usuario.getLanguage());
    }

    /** El nombre del PDF resultante es el del documento, sea cual sea la extensión del recurso. */
    private static String nombrePdf(String documentoFileName) {
        String nombre = Path.of(documentoFileName).getFileName().toString();
        return nombre.replaceAll("\\.xml$", ".pdf");
    }

    private static byte[] leerRecurso(Class<?> clase, String documentoFileName) {
        try (InputStream in = clase.getResourceAsStream(documentoFileName)) {
            if (in == null) {
                throw new IOException("No se encontró el recurso: " + documentoFileName);
            }
            return in.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException("Error al cargar el documento PDF: " + documentoFileName, e);
        }
    }

}
