package com.educaflow.tiposexpedientes.support;

import com.educaflow.base.infrastructure.pdf.DocumentoPdf;
import com.educaflow.base.infrastructure.pdf.DocumentoPdfFactory;
import com.educaflow.base.infrastructure.pdfgenerator.PdfGeneratorFactory;
import com.educaflow.common.buildtools.files.tipoexpediente.TipoExpedienteInstanceFile;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Los documentos PDF de un tipo de expediente: los ficheros de su carpeta {@code documentospdf/} y,
 * de cada uno, las expresiones que el runtime va a evaluar para producirlo.
 *
 * <p>Un documento está en la carpeta <b>o</b> como XML de definición (raíz {@code <documento>}, que
 * el build deja resuelto en el classpath con {@code resolvePdfDocuments} y del que la aplicación
 * genera el PDF en runtime) <b>o</b> directamente como PDF versionado, que se rellena. Los
 * {@code _*.xml} son fragmentos incluidos desde otros documentos y no son documentos, y la
 * subcarpeta {@code originales/} es material de partida que no se rellena: ni unos ni otra son
 * documentos.
 *
 * <p>Las expresiones se sacan <b>del classpath</b>, con el mismo lector que el runtime: del XML
 * resuelto con el parser del generador ({@code PdfGenerator.getExpresiones}: cada {@code nombreCampo},
 * cada {@code ${expresion;n}} de los dos idiomas y cada {@code visible}, con los fragmentos ya
 * expandidos), y del PDF versionado con {@link DocumentoPdfFactory} (los nombres de sus campos de
 * formulario, que es lo que {@code DocumentoPdfUtil.generate} evalúa). Así no se reimplementa
 * ningún parseo y se comprueba exactamente lo que se va a evaluar.
 */
public final class DocumentosDelTipo {

    /** El nombre de la carpeta de documentos de un tipo de expediente, en la raíz de su carpeta de versión. */
    public static final String NOMBRE_CARPETA = "documentospdf";

    /**
     * Un documento del tipo.
     *
     * @param fuente  el fichero versionado del que sale: el XML de definición o el propio PDF.
     * @param recurso la ruta absoluta de classpath del documento (el XML resuelto o el PDF), la misma
     *                que lleva la constante del enum {@code TipoDocumentoPdf} de la entidad.
     */
    public record Documento(TipoExpedienteInstanceFile tipo, Path fuente, String recurso) {

        /** Ruta relativa legible del fichero versionado, para los mensajes de error. */
        public String fichero() {
            return TiposExpediente.rel(fuente);
        }
    }

    private DocumentosDelTipo() {}

    /** La carpeta {@code documentospdf/} del tipo, exista o no. */
    public static Path carpeta(TipoExpedienteInstanceFile tipo) {
        return TiposExpediente.carpeta(tipo).resolve(NOMBRE_CARPETA);
    }

    /** Los documentos del tipo, en orden de nombre de fichero. Vacío si el tipo no tiene la carpeta. */
    public static List<Documento> de(TipoExpedienteInstanceFile tipo) {
        Path carpeta = carpeta(tipo);
        if (!Files.isDirectory(carpeta)) {
            return List.of();
        }

        List<Documento> documentos = new ArrayList<>();
        try (Stream<Path> ficheros = Files.list(carpeta)) {
            for (Path fichero : ficheros.filter(Files::isRegularFile).sorted().toList()) {
                nombreRecurso(fichero).ifPresent(recurso -> documentos.add(new Documento(tipo, fichero,
                        "/com/educaflow/" + TiposExpediente.rel(carpeta) + "/" + recurso)));
            }
        } catch (IOException ex) {
            throw new IllegalStateException("No se puede listar " + carpeta, ex);
        }

        return documentos;
    }

    /**
     * Las expresiones del documento, tal cual las va a evaluar el runtime. Vacío si el recurso no está
     * en el classpath (no se ha compilado).
     */
    public static Optional<List<String>> expresiones(Documento documento) {
        try (InputStream in = DocumentosDelTipo.class.getResourceAsStream(documento.recurso())) {
            if (in == null) {
                return Optional.empty();
            }
            byte[] bytes = in.readAllBytes();
            if (documento.recurso().endsWith(".xml")) {
                return Optional.of(PdfGeneratorFactory.getPdfGenerator().getExpresiones(bytes));
            }
            String nombre = documento.recurso().substring(documento.recurso().lastIndexOf('/') + 1);
            DocumentoPdf pdf = DocumentoPdfFactory.getDocumentoPdf(bytes, nombre);

            return Optional.of(pdf.getNombreCamposFormulario());
        } catch (IOException ex) {
            throw new IllegalStateException("No se puede leer el recurso " + documento.recurso(), ex);
        }
    }

    /** El nombre del recurso de classpath que produce el fichero, o vacío si el fichero no es un documento. */
    private static Optional<String> nombreRecurso(Path fichero) {
        String nombre = fichero.getFileName().toString();
        if (nombre.endsWith(".pdf")) {
            return Optional.of(nombre);
        }
        if (nombre.endsWith(".xml") && !nombre.startsWith("_") && esDocumento(fichero)) {
            return Optional.of(nombre);
        }

        return Optional.empty();
    }

    /** Si el XML tiene raíz {@code <documento>}: el mismo criterio con el que el build decide qué resolver. */
    private static boolean esDocumento(Path xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

            return factory.newDocumentBuilder().parse(xml.toFile()).getDocumentElement().getTagName().equals("documento");
        } catch (Exception ex) {
            throw new IllegalStateException("XML no parseable: " + xml + " -> " + ex.getMessage(), ex);
        }
    }
}
