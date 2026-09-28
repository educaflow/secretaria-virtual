package com.educaflow.tiposexpedientes.documentos;

import com.educaflow.common.buildtools.files.tipoexpediente.TipoExpedienteInstanceFile;
import com.educaflow.tiposexpedientes.support.DocumentosDelTipo;
import com.educaflow.tiposexpedientes.support.DocumentosDelTipo.Documento;
import com.educaflow.tiposexpedientes.support.DomainsDelTipo;
import com.educaflow.tiposexpedientes.support.ExpresionGroovy;
import com.educaflow.tiposexpedientes.support.TiposExpediente;
import com.educaflow.tiposexpedientes.support.Violacion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Las expresiones Groovy de los documentos PDF de cada tipo de expediente compilan contra su
 * entidad.
 *
 * <p>Cada {@code nombreCampo}, cada {@code ${expresion}} y cada {@code visible} de un XML de
 * {@code documentospdf/} es una expresión que el runtime evalúa con {@code GroovyShell} sobre
 * {@code self} (la entidad del expediente) y {@code now} al generar el PDF. Nadie las mira antes: el
 * build solo resuelve el XML, y el compilador de Java no sabe nada de lo que hay dentro de un
 * atributo XML.
 *
 * <p>Y cuando una revienta al evaluarse ya es tarde: el generador aborta el evento con una
 * {@code RuntimeException} (y en un PDF versionado que se rellena, {@code EvaluatorImplGroovy} se
 * calla y el campo sale <b>vacío</b>). En los dos casos se descubre con el expediente en marcha, y
 * es el fallo más fácil de introducir: basta renombrar un campo del {@code domains.xml} o duplicar
 * una versión y dejar en el XML el FQCN del enum de la anterior ({@code recetas/versionado.md}).
 *
 * <p>De ahí la regla:
 *
 * <ul>
 *   <li><b>P1</b>: toda expresión de todo documento del tipo compila con {@code @TypeChecked}
 *       tomando {@code self} como la entidad del {@code domains.xml} y {@code now} como
 *       {@code LocalDateTime}. Comprueba estáticamente lo que el runtime haría dinámicamente:
 *       que cada propiedad existe en la entidad, que cada FQCN resuelve y que la sintaxis es
 *       válida. No evalúa nada, así que no necesita ni instancia ni base de datos.</li>
 * </ul>
 *
 * <p>Las expresiones se leen <b>del classpath</b>, con el mismo lector que el runtime: del XML
 * resuelto con el parser del generador de PDF, y de los PDF versionados con el lector de formularios.
 * Es literalmente el conjunto que se va a evaluar, con los fragmentos ya expandidos
 * ({@link DocumentosDelTipo}). Lo que P1 <b>no</b> comprueba es lo que depende de los datos del
 * expediente: una relación a {@code null} en mitad de una cadena, o un patrón de
 * {@code DateTimeFormatter} mal escrito, siguen fallando solo en runtime.
 *
 * <p>Que el tipo tenga entidad lo vigila {@code M1}; si no la tiene, aquí no se dice nada para no
 * reportarlo dos veces.
 *
 * <p><b>Estos tests se escriben A MANO.</b> Este fichero es la fuente de verdad y se edita
 * directamente.
 */
class ExpresionesDeDocumentoTest {

    @Test
    @DisplayName("P1: toda expresión Groovy de los documentos PDF del tipo compila contra su entidad (self) y now")
    void p1_todaExpresionDeLosDocumentosCompilaContraLaEntidad() {
        List<Violacion> violaciones = new ArrayList<>();

        for (TipoExpedienteInstanceFile tipo : TiposExpediente.all()) {
            Optional<String> entidad = DomainsDelTipo.fqcnEntidad(tipo);
            if (entidad.isEmpty()) {
                continue;
            }

            for (Documento documento : DocumentosDelTipo.de(tipo)) {
                Optional<List<String>> expresiones = DocumentosDelTipo.expresiones(documento);
                if (expresiones.isEmpty()) {
                    violaciones.add(new Violacion(tipo.getCode(), documento.fichero(),
                            "su recurso no está en el classpath de los tests: " + documento.recurso()
                            + "\n      El build lo resuelve (resolvePdfDocuments) o lo copia antes de los tests; ¿falta compilar?"));
                    continue;
                }

                for (String expresion : expresiones.get()) {
                    ExpresionGroovy.errorDeCompilacion(expresion, entidad.get()).ifPresent(error ->
                            violaciones.add(new Violacion(tipo.getCode(), documento.fichero(),
                                    "la expresión «" + expresion + "» no compila contra la entidad del tipo."
                                    + "\n      " + error.replace("\n", "\n      ")
                                    + "\n      Se evalúa con self = " + entidad.get() + " y now = java.time.LocalDateTime."
                                    + " En runtime el generador aborta el evento con este error (y un PDF versionado saldría con el campo vacío).")));
                }
            }
        }

        Violacion.assertNone("[P1] Toda expresión Groovy de los documentos PDF de un tipo de expediente (nombreCampo,"
                + " ${expresion} y visible) debe compilar contra su entidad: cada propiedad debe existir en ella y cada FQCN"
                + " debe resolver. En runtime se evalúan sin comprobar nada y el fallo se descubre con el expediente en marcha.",
                violaciones);
    }
}
