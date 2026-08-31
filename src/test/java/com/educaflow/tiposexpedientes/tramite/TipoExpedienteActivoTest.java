package com.educaflow.tiposexpedientes.tramite;

import com.educaflow.common.buildtools.files.tipoexpediente.TipoExpedienteInstanceFileFinder;
import com.educaflow.common.buildtools.files.tramite.TramiteInstanceFile;
import com.educaflow.common.buildtools.files.tramite.TramiteInstanceFileFinder;
import com.educaflow.common.buildtools.files.tramite.TramitesLayout;
import com.educaflow.tiposexpedientes.support.TiposExpediente;
import com.educaflow.tiposexpedientes.support.Violacion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * El {@code <defaultTipoExpediente>} de un {@code TramiteInstance.xml} nombra una carpeta de
 * versión que existe de verdad bajo el trámite y que tiene dentro su
 * {@code TipoExpedienteInstance.xml}.
 *
 * <p>Es el único enlace entre un trámite y su versión vigente, y hoy se rompe <b>en silencio</b> en
 * los tres eslabones de la cadena:
 * <ol>
 * <li>{@code TramiteInstanceFile.getDefaultTipoExpedienteCode()} busca una carpeta con ese nombre
 * bajo el trámite y, si no encuentra ninguna, <b>asume que el valor ya es un code</b> y lo devuelve
 * tal cual. Un nombre de carpeta mal escrito no da error: se convierte en un code inventado.</li>
 * <li>El data-init generado lo emite como
 * {@code <tramite code="…" defaultTipoExpediente="<ese code inventado>"/>}, y su bind es
 * {@code search="self.code = :tipoExpediente" create="false"}: si no hay ningún
 * {@code TipoExpediente} con ese code, el import no falla, simplemente deja la columna a
 * {@code null}.</li>
 * <li>El fallo aparece al final, en runtime y solo si alguien abre el trámite, como el
 * {@code RuntimeException("No existe el tipo de expediente para el tramite con idTramite: …")} de
 * {@code ExpedienteController.getTipoExpedienteFromIdTramite()}.</li>
 * </ol>
 *
 * <p>Por eso la regla se comprueba aquí, contra el árbol de fuentes: es el único punto de la cadena
 * en el que el error se puede señalar en el fichero que hay que editar.
 *
 * <p>La regla es <b>más estricta</b> que el generador a propósito. El generador acepta también que
 * el valor sea directamente el {@code code} del tipo de expediente; el proyecto exige que sea el
 * nombre de la carpeta de versión, que es lo que se puede verificar. Un code escrito a mano no se
 * distingue de una errata hasta que se arranca la aplicación.
 *
 * <p>Un trámite sin {@code <defaultTipoExpediente>} (o con el tag en blanco) no incumple nada: es
 * la forma declarada de decir que el trámite todavía no tiene versión vigente, y entonces no se
 * genera su data-init.
 *
 * <p><b>Estos tests se escriben A MANO.</b> Este fichero es la fuente de verdad y se edita
 * directamente.
 */
class TipoExpedienteActivoTest {

    @Test
    @DisplayName("T1: el <defaultTipoExpediente> de cada trámite nombra una carpeta suya con un TipoExpedienteInstance.xml")
    void t1_elDefaultTipoExpedienteNombraUnaCarpetaDeVersionQueExiste() {
        List<Violacion> violaciones = new ArrayList<>();

        for (TramiteInstanceFile tramite : tramites()) {
            String declarado = defaultTipoExpedienteDeclarado(tramite.getPath());
            if (declarado == null) {
                continue;
            }

            List<Path> candidatas = carpetasDeVersionLlamadas(tramite, declarado);

            if (candidatas.isEmpty()) {
                violaciones.add(new Violacion(tramite.getCode(), TiposExpediente.rel(tramite.getPath()),
                        "declara <defaultTipoExpediente>" + declarado + "</defaultTipoExpediente>, pero bajo el"
                        + " trámite no hay ninguna carpeta '" + declarado + "' con un "
                        + TipoExpedienteInstanceFileFinder.TIPO_EXPEDIENTE_XML_NAME + " dentro."
                        + " Las carpetas de versión que hay son: " + carpetasDeVersion(tramite) + "."
                        + " Nadie va a avisar de esto: el data-init dejará la columna defaultTipoExpediente a null"
                        + " y el trámite reventará al abrirlo con 'No existe el tipo de expediente para el tramite'"));
            } else if (candidatas.size() > 1) {
                violaciones.add(new Violacion(tramite.getCode(), TiposExpediente.rel(tramite.getPath()),
                        "declara <defaultTipoExpediente>" + declarado + "</defaultTipoExpediente>, que es ambiguo:"
                        + " bajo el trámite hay " + candidatas.size() + " carpetas '" + declarado + "' con un "
                        + TipoExpedienteInstanceFileFinder.TIPO_EXPEDIENTE_XML_NAME + " dentro: "
                        + relativas(candidatas) + ". Renombra las carpetas para que el nombre de versión sea único"));
            }
        }

        Violacion.assertNone("[T1] El <defaultTipoExpediente> de un TramiteInstance.xml debe ser el nombre de una"
                + " carpeta de versión que exista bajo ese trámite y que tenga dentro su "
                + TipoExpedienteInstanceFileFinder.TIPO_EXPEDIENTE_XML_NAME
                + ": es el enlace del trámite con su versión vigente y, si no resuelve, falla en silencio"
                + " hasta el runtime.", violaciones);
    }

    // -----------------------------------------------------------------------------------------
    // Ayudas
    // -----------------------------------------------------------------------------------------

    /** Todos los trámites del proyecto, descubiertos y parseados con las clases del generador. */
    private static List<TramiteInstanceFile> tramites() {
        TramitesLayout tramitesLayout = layout();

        List<TramiteInstanceFile> tramites = new TramiteInstanceFileFinder(tramitesLayout).findTramitesFile();
        if (tramites.isEmpty()) {
            throw new IllegalStateException("No se encontró ningún " + TramiteInstanceFile.TRAMITE_XML_NAME
                    + " bajo " + tramitesLayout.getRootPackagePath() + " (¿directorio de trabajo incorrecto?)");
        }

        return tramites;
    }

    private static TramitesLayout layout() {
        return new TramitesLayout(TiposExpediente.origen(), TramitesLayout.PAQUETE_RAIZ_POR_DEFECTO);
    }

    /**
     * El {@code <defaultTipoExpediente>} <b>tal cual está escrito</b> en el XML, o null si no está o
     * está en blanco.
     *
     * <p>Se lee con JAXP y no con {@code TramiteInstanceFile}: su
     * {@code getDefaultTipoExpedienteCode()} no devuelve el valor declarado sino el ya derivado, y
     * precisamente la derivación que hay que denunciar —la del valor que no resuelve a ninguna
     * carpeta— es la que lo devuelve sin tocar, indistinguible de un acierto.
     */
    private static String defaultTipoExpedienteDeclarado(Path tramiteXmlFile) {
        NodeList nodos = doc(tramiteXmlFile).getElementsByTagName("defaultTipoExpediente");
        if (nodos.getLength() == 0) {
            return null;
        }

        String valor = ((Element) nodos.item(0)).getTextContent();
        if ((valor == null) || valor.isBlank()) {
            return null;
        }

        return valor.trim();
    }

    /**
     * Las carpetas con ese nombre que hay bajo el trámite, a cualquier profundidad, que tienen
     * dentro un {@code TipoExpedienteInstance.xml}.
     *
     * <p>Es la misma búsqueda que hace el generador al resolver el code, para que el test denuncie
     * exactamente lo que él no encuentra: las carpetas intermedias son solo agrupación.
     */
    private static List<Path> carpetasDeVersionLlamadas(TramiteInstanceFile tramite, String nombreCarpeta) {
        List<Path> carpetas = new ArrayList<>();
        for (Path tipoExpedienteXmlFile : tiposExpedienteXmlFiles(tramite)) {
            if (tipoExpedienteXmlFile.getParent().getFileName().toString().equals(nombreCarpeta)) {
                carpetas.add(tipoExpedienteXmlFile.getParent());
            }
        }

        return carpetas;
    }

    /** Los nombres de carpeta de las versiones que sí existen, para poder decir cuál se quiso poner. */
    private static List<String> carpetasDeVersion(TramiteInstanceFile tramite) {
        List<String> nombres = new ArrayList<>();
        for (Path tipoExpedienteXmlFile : tiposExpedienteXmlFiles(tramite)) {
            nombres.add(tipoExpedienteXmlFile.getParent().getFileName().toString());
        }

        return nombres;
    }

    /** Todos los TipoExpedienteInstance.xml que cuelgan de la carpeta del trámite. */
    private static List<Path> tiposExpedienteXmlFiles(TramiteInstanceFile tramite) {
        Path carpetaTramite = tramite.getPath().getParent();

        try (Stream<Path> arbol = Files.walk(carpetaTramite)) {
            return arbol.filter(Files::isRegularFile)
                    .filter(fichero -> fichero.getFileName().toString()
                            .equals(TipoExpedienteInstanceFileFinder.TIPO_EXPEDIENTE_XML_NAME))
                    .sorted()
                    .toList();
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo recorrer " + carpetaTramite, ex);
        }
    }

    private static List<String> relativas(List<Path> paths) {
        List<String> nombres = new ArrayList<>();
        for (Path path : paths) {
            nombres.add(TiposExpediente.rel(path));
        }

        return nombres;
    }

    private static Document doc(Path path) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false); // el TramiteInstance.xml no lleva namespace
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            DocumentBuilder builder = factory.newDocumentBuilder();

            return builder.parse(path.toFile());
        } catch (Exception ex) {
            throw new IllegalStateException(TramiteInstanceFile.TRAMITE_XML_NAME + " no parseable: " + path
                    + " -> " + ex.getMessage(), ex);
        }
    }
}
