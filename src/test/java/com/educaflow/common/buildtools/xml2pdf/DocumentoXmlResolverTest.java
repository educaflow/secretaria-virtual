package com.educaflow.common.buildtools.xml2pdf;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * El paso de build que deja autocontenido el XML de un documento PDF de un trámite: lo valida
 * contra el esquema de su tipo, expande sus {@code <include>} y completa los {@code <valenciano>}
 * que falten.
 *
 * <p>El árbol de fuentes se monta en un directorio temporal bajo el paquete raíz de los trámites.
 * Vive en este paquete porque {@code DocumentoXmlResolver.run} no es público.
 */
class DocumentoXmlResolverTest {

    private static final Path APERTIUM = Path.of("/usr/bin/apertium");
    private static final String PAQUETE_RAIZ = "src/main/java/com/educaflow/tramites";
    private static final String CARPETA_DOCUMENTO = PAQUETE_RAIZ + "/prueba/v1/documentospdf";
    private static final String CARPETA_COMPARTIDA = PAQUETE_RAIZ + "/shared";
    /** De la carpeta del documento a la carpeta compartida, que es la que cuelga del paquete raíz. */
    private static final String HASTA_LA_COMPARTIDA = "../../../shared/";

    @TempDir
    Path raiz;

    @Test
    void unDocumentoDeTextoSeValidaContraElEsquemaDeSuTipo() {
        Path documento = escribir(CARPETA_DOCUMENTO + "/documento.xml", """
                <documentoTexto>
                    <titulo><valenciano>Titol</valenciano><castellano>Titulo</castellano></titulo>
                    <parrafo><valenciano>Hola</valenciano><castellano>Hola</castellano></parrafo>
                    <seccion><castellano>Esto es de un formulario</castellano></seccion>
                </documentoTexto>
                """);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> resolver(documento));

        assertTrue(ex.getMessage().contains("documentoTexto.xsd"),
                "el mensaje no dice contra qué esquema se validó: " + ex.getMessage());
        assertTrue(ex.getMessage().contains("seccion"),
                "el mensaje no dice qué elemento sobra: " + ex.getMessage());
    }

    @Test
    void unDocumentoDeFormularioSeValidaContraElEsquemaDelFormulario() {
        Path documento = escribir(CARPETA_DOCUMENTO + "/documento.xml", """
                <documentoFormulario>
                    <titulo><valenciano>Titol</valenciano><castellano>Titulo</castellano></titulo>
                    <lista><item><castellano>Esto es de un documento de texto</castellano></item></lista>
                </documentoFormulario>
                """);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> resolver(documento));

        assertTrue(ex.getMessage().contains("documentoFormulario.xsd"),
                "el mensaje no dice contra qué esquema se validó: " + ex.getMessage());
        assertTrue(ex.getMessage().contains("lista"),
                "el mensaje no dice qué elemento sobra: " + ex.getMessage());
    }

    @Test
    void unIncludeSeSustituyePorElContenidoDelFragmento() {
        escribir(CARPETA_COMPARTIDA + "/_cierre.xml", """
                <fragmento>
                    <parrafo><valenciano>Del fragment</valenciano><castellano>Del fragmento</castellano></parrafo>
                </fragmento>
                """);
        Path documento = escribir(CARPETA_DOCUMENTO + "/documento.xml", """
                <documentoTexto>
                    <titulo><valenciano>Titol</valenciano><castellano>Titulo</castellano></titulo>
                    <parrafo><valenciano>Del document</valenciano><castellano>Del documento</castellano></parrafo>
                    <include href="%s_cierre.xml"/>
                </documentoTexto>
                """.formatted(HASTA_LA_COMPARTIDA));

        String resuelto = resolver(documento);

        assertFalse(resuelto.contains("<include"), "el include sigue ahí:\n" + resuelto);
        assertTrue(resuelto.indexOf("Del documento") < resuelto.indexOf("Del fragmento"),
                "el contenido del fragmento no ocupa el sitio del include:\n" + resuelto);
    }

    @Test
    void unDocumentoDeTextoSinTituloNoValida() {
        Path documento = escribir(CARPETA_DOCUMENTO + "/documento.xml", """
                <documentoTexto>
                    <parrafo><valenciano>Cos</valenciano><castellano>Cuerpo</castellano></parrafo>
                </documentoTexto>
                """);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> resolver(documento));

        assertTrue(ex.getMessage().contains("documentoTexto.xsd"),
                "el mensaje no dice contra qué esquema se validó: " + ex.getMessage());
        assertTrue(ex.getMessage().contains("titulo"),
                "el mensaje no dice qué elemento falta: " + ex.getMessage());
    }

    @Test
    void unFragmentoDeTextoNoLlevaTitulo() {
        escribir(CARPETA_COMPARTIDA + "/_cabecera.xml", """
                <fragmento>
                    <titulo><castellano>El titulo es del documento</castellano></titulo>
                </fragmento>
                """);
        Path documento = escribir(CARPETA_DOCUMENTO + "/documento.xml", """
                <documentoTexto>
                    <titulo><valenciano>Titol</valenciano><castellano>Titulo</castellano></titulo>
                    <include href="%s_cabecera.xml"/>
                </documentoTexto>
                """.formatted(HASTA_LA_COMPARTIDA));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> resolver(documento));

        assertTrue(ex.getMessage().contains("_cabecera.xml"),
                "el mensaje no dice qué fichero está mal: " + ex.getMessage());
    }

    @Test
    void unFragmentoDeFormularioIncluidoEnUnDocumentoDeTextoAborta() {
        escribir(CARPETA_COMPARTIDA + "/_seccion.xml", """
                <fragmento>
                    <seccion><castellano>Datos</castellano></seccion>
                </fragmento>
                """);
        Path documento = escribir(CARPETA_DOCUMENTO + "/documento.xml", """
                <documentoTexto>
                    <titulo><valenciano>Titol</valenciano><castellano>Titulo</castellano></titulo>
                    <include href="%s_seccion.xml"/>
                </documentoTexto>
                """.formatted(HASTA_LA_COMPARTIDA));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> resolver(documento));

        assertTrue(ex.getMessage().contains("documentoTexto.xsd"),
                "el fragmento debe validarse contra el esquema del documento que lo incluye: " + ex.getMessage());
        assertTrue(ex.getMessage().contains("_seccion.xml"),
                "el mensaje no dice qué fichero está mal: " + ex.getMessage());
    }

    @Test
    void unaFilaConMasCeldasQueColumnasAbortaLaResolucion() {
        Path documento = escribir(CARPETA_DOCUMENTO + "/documento.xml", """
                <documentoTexto>
                    <titulo><valenciano>Titol</valenciano><castellano>Titulo</castellano></titulo>
                    <tabla columnas="2">
                        <fila>
                            <parrafo><valenciano>Una</valenciano><castellano>Una</castellano></parrafo>
                            <parrafo><valenciano>Dos</valenciano><castellano>Dos</castellano></parrafo>
                            <parrafo><valenciano>Tres</valenciano><castellano>Tres</castellano></parrafo>
                        </fila>
                    </tabla>
                </documentoTexto>
                """);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> resolver(documento));

        assertTrue(ex.getMessage().contains("2 columnas") && ex.getMessage().contains("3 hijos"),
                "el mensaje no dice cuántas celdas hay ni cuántas se esperaban: " + ex.getMessage());
    }

    @Test
    void elCampoFirmaDeUnEspacioYDeUnTextoValidaYLlegaAlXmlResuelto() {
        Path texto = escribir(CARPETA_DOCUMENTO + "/documento.xml", """
                <documentoTexto>
                    <titulo><valenciano>Titol</valenciano><castellano>Titulo</castellano></titulo>
                    <espacio alto="80" campoFirma="firmaDirector"/>
                </documentoTexto>
                """);
        assertTrue(resolver(texto).contains("campoFirma=\"firmaDirector\""));

        Path formulario = escribir(CARPETA_DOCUMENTO + "/documento.xml", """
                <documentoFormulario>
                    <titulo><valenciano>Titol</valenciano><castellano>Titulo</castellano></titulo>
                    <seccion>
                        <valenciano>Signat</valenciano><castellano>Firmado</castellano>
                        <fila>
                            <texto colspan="12" rowSpan="4" campoFirma="firmaSolicitante"><valenciano>Signatura</valenciano><castellano>Firma</castellano></texto>
                        </fila>
                    </seccion>
                </documentoFormulario>
                """);
        assertTrue(resolver(formulario).contains("campoFirma=\"firmaSolicitante\""));
    }

    @Test
    void dosCamposFirmaConElMismoNombreAbortanLaResolucion() {
        Path documento = escribir(CARPETA_DOCUMENTO + "/documento.xml", """
                <documentoTexto>
                    <titulo><valenciano>Titol</valenciano><castellano>Titulo</castellano></titulo>
                    <espacio alto="80" campoFirma="firmaDirector"/>
                    <espacio alto="80" campoFirma="firmaDirector"/>
                </documentoTexto>
                """);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> resolver(documento));

        assertTrue(ex.getMessage().contains("firmaDirector"), "el mensaje no dice qué nombre se repite: " + ex.getMessage());
    }

    @Test
    void unCampoFirmaRepetidoEntreElDocumentoYUnFragmentoTambienAborta() {
        escribir(CARPETA_COMPARTIDA + "/_pieFirma.xml", """
                <fragmento>
                    <espacio alto="80" campoFirma="firmaDirector"/>
                </fragmento>
                """);
        Path documento = escribir(CARPETA_DOCUMENTO + "/documento.xml", """
                <documentoTexto>
                    <titulo><valenciano>Titol</valenciano><castellano>Titulo</castellano></titulo>
                    <espacio alto="80" campoFirma="firmaDirector"/>
                    <include href="%s_pieFirma.xml"/>
                </documentoTexto>
                """.formatted(HASTA_LA_COMPARTIDA));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> resolver(documento));

        assertTrue(ex.getMessage().contains("firmaDirector"), "el mensaje no dice qué nombre se repite: " + ex.getMessage());
    }

    @Test
    void unCampoFirmaQueNoEsUnIdentificadorAborta() {
        Path documento = escribir(CARPETA_DOCUMENTO + "/documento.xml", """
                <documentoTexto>
                    <titulo><valenciano>Titol</valenciano><castellano>Titulo</castellano></titulo>
                    <espacio alto="80" campoFirma="firma del director"/>
                </documentoTexto>
                """);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> resolver(documento));

        assertTrue(ex.getMessage().contains("campoFirma"), ex.getMessage());
    }

    @Test
    void elCampoFirmaSoloLoAdmiteElTextoDeUnFormulario() {
        Path documento = escribir(CARPETA_DOCUMENTO + "/documento.xml", """
                <documentoFormulario>
                    <titulo><valenciano>Titol</valenciano><castellano>Titulo</castellano></titulo>
                    <seccion>
                        <valenciano>Signat</valenciano><castellano>Firmado</castellano>
                        <fila>
                            <campo nombreCampo="self.x" colspan="12" campoFirma="firmaSolicitante"><valenciano>Signatura</valenciano><castellano>Firma</castellano></campo>
                        </fila>
                    </seccion>
                </documentoFormulario>
                """);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> resolver(documento));

        assertTrue(ex.getMessage().contains("campoFirma"), ex.getMessage());
    }

    @Test
    void elValencianoQueFaltaLoCompletaElTraductor() {
        assumeTrue(Files.isExecutable(APERTIUM), "sin " + APERTIUM + " no se puede traducir");
        Path documento = escribir(CARPETA_DOCUMENTO + "/documento.xml", """
                <documentoTexto>
                    <titulo><valenciano>Titol</valenciano><castellano>Titulo</castellano></titulo>
                    <parrafo><castellano>El centro certifica las horas.</castellano></parrafo>
                </documentoTexto>
                """);

        String resuelto = resolver(documento);

        assertTrue(resuelto.contains("<valenciano>El centre certifica les hores.</valenciano>"),
                "no se ha traducido el castellano al valenciano:\n" + resuelto);
        assertTrue(resuelto.indexOf("<valenciano>") < resuelto.indexOf("<castellano>"),
                "el valenciano tiene que ir delante del castellano, como exige el esquema:\n" + resuelto);
    }

    // ------------------------------------------------------------------ apoyo

    private String resolver(Path documento) {
        Path salida = raiz.resolve("resuelto/documento.xml");
        try {
            new DocumentoXmlResolver(APERTIUM.toString()).run(documento.toString(), salida.toString());
            return Files.readString(salida, StandardCharsets.UTF_8);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private Path escribir(String rutaRelativa, String contenido) {
        Path fichero = raiz.resolve(rutaRelativa);
        try {
            Files.createDirectories(fichero.getParent());
            Files.writeString(fichero, contenido, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
        return fichero;
    }
}
