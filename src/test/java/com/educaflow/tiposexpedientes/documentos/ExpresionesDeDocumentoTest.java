package com.educaflow.tiposexpedientes.documentos;

import com.educaflow.common.buildtools.files.tipoexpediente.TipoExpedienteInstanceFile;
import com.educaflow.tiposexpedientes.support.DocumentosDelTipo;
import com.educaflow.tiposexpedientes.support.DocumentosDelTipo.Documento;
import com.educaflow.tiposexpedientes.support.DomainsDelTipo;
import com.educaflow.tiposexpedientes.support.ExpresionGroovy;
import com.educaflow.tiposexpedientes.support.ExpresionGroovy.Compilacion;
import com.educaflow.tiposexpedientes.support.ExpresionGroovy.NavegacionInsegura;
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
 * <p>Cada {@code nombreCampo} y cada {@code ${expresion;n}} de un XML de {@code documentospdf/}
 * acaba en el PDF como un campo de formulario cuyo nombre es la expresión, y en runtime
 * {@code DocumentoPdfUtil.generate} evalúa todos esos nombres con {@code GroovyShell} sobre
 * {@code self} (la entidad del expediente) y {@code now}. Nadie las mira antes: el build solo
 * dibuja el PDF vacío, y el compilador de Java no sabe nada de lo que hay dentro de un atributo XML.
 *
 * <p>Y cuando una revienta al evaluarse nadie se entera: {@code EvaluatorImplGroovy} captura la
 * excepción, la escribe por consola y sigue, así que el evento termina bien y el documento sale con
 * el campo <b>vacío</b>. Es el fallo más caro de esta carpeta, porque se descubre leyendo un PDF ya
 * emitido, y el más fácil de introducir: basta renombrar un campo del {@code domains.xml} o duplicar
 * una versión y dejar en el XML el FQCN del enum de la anterior ({@code recetas/versionado.md}).
 *
 * <p>De ahí las dos reglas:
 *
 * <ul>
 *   <li><b>P1</b>: toda expresión de todo documento del tipo compila con {@code @TypeChecked}
 *       tomando {@code self} como la entidad del {@code domains.xml} y {@code now} como
 *       {@code LocalDateTime}. Comprueba estáticamente lo que el runtime haría dinámicamente:
 *       que cada propiedad existe en la entidad, que cada FQCN resuelve y que la sintaxis es
 *       válida. No evalúa nada, así que no necesita ni instancia ni base de datos.</li>
 *   <li><b>P2</b>: toda navegación por una propiedad que puede ser {@code null} usa {@code ?.}.
 *       {@code self.personaInteresada.nombre} revienta con un NPE si la persona no está, y en el
 *       PDF sale lo mismo que con {@code self.personaInteresada?.nombre}: el campo vacío. La
 *       diferencia es que la primera es un error y la segunda una decisión escrita en el XML. Qué
 *       puede ser {@code null} lo dice el modelo: todo lo que no sea {@code required="true"} (que
 *       Axelor compila a {@code @NotNull}) ni un primitivo. Y como el resultado de un {@code ?.}
 *       es {@code null} cuando su receptor lo es, todo paso posterior a un {@code ?.} también ha de
 *       llevarlo ({@code self.a?.b?.c}).</li>
 * </ul>
 *
 * <p>Las expresiones se leen del <b>PDF del classpath</b>, con el mismo lector que el runtime, no
 * del XML: es literalmente el conjunto que se va a evaluar, con los fragmentos ya expandidos y los
 * PDF versionados incluidos ({@link DocumentosDelTipo}). Lo que P1 y P2 <b>no</b> comprueban es lo
 * que depende de los valores y no de los tipos: un patrón de {@code DateTimeFormatter} mal escrito
 * sigue fallando solo en runtime, y el resultado de un método no se analiza.
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
                            "su PDF no está en el classpath de los tests: " + documento.recurso()
                            + "\n      El build lo genera (generatePdfDocuments) o lo copia antes de los tests; ¿falta compilar?"));
                    continue;
                }

                for (String expresion : expresiones.get()) {
                    ExpresionGroovy.compilar(expresion, entidad.get()).error().ifPresent(error ->
                            violaciones.add(new Violacion(tipo.getCode(), documento.fichero(),
                                    "la expresión «" + expresion + "» no compila contra la entidad del tipo."
                                    + "\n      " + error.replace("\n", "\n      ")
                                    + "\n      Se evalúa con self = " + entidad.get() + " y now = java.time.LocalDateTime."
                                    + " En runtime este fallo es silencioso: log y campo vacío en el PDF.")));
                }
            }
        }

        Violacion.assertNone("[P1] Toda expresión Groovy de los documentos PDF de un tipo de expediente (nombreCampo y"
                + " ${expresion;n}) debe compilar contra su entidad: cada propiedad debe existir en ella y cada FQCN"
                + " debe resolver. En runtime se evalúan sin comprobar nada y el fallo deja el campo vacío en silencio.",
                violaciones);
    }

    @Test
    @DisplayName("P2: toda navegación de una expresión por una propiedad que puede ser null (no required) usa ?.")
    void p2_todaNavegacionPorUnaPropiedadQuePuedeSerNullUsaNavegacionSegura() {
        List<Violacion> violaciones = new ArrayList<>();

        for (TipoExpedienteInstanceFile tipo : TiposExpediente.all()) {
            Optional<String> entidad = DomainsDelTipo.fqcnEntidad(tipo);
            if (entidad.isEmpty()) {
                continue;
            }

            for (Documento documento : DocumentosDelTipo.de(tipo)) {
                // Si el PDF no está o la expresión no compila lo dice P1; aquí solo se miran las que compilan.
                for (String expresion : DocumentosDelTipo.expresiones(documento).orElse(List.of())) {
                    Compilacion compilacion = ExpresionGroovy.compilar(expresion, entidad.get());
                    for (NavegacionInsegura navegacion : compilacion.navegacionesInseguras()) {
                        violaciones.add(new Violacion(tipo.getCode(), documento.fichero(),
                                "la expresión «" + expresion + "» navega sin ?. por «" + navegacion.receptor()
                                + "», que puede ser null: «" + navegacion.paso() + "» revienta con un NPE."
                                + "\n      " + navegacion.propiedad() + " de " + navegacion.clase() + " no es required="
                                + "\"true\" en el modelo" + (navegacion.receptor().contains("?.")
                                        ? ", y además viene tras un ?., cuyo resultado es null cuando lo es su receptor"
                                        : "") + "."
                                + "\n      Escribe «" + navegacion.receptor() + "?." + navegacion.paso().substring(
                                        navegacion.receptor().length() + 1)
                                + "» (o marca el campo required en el domains.xml si nunca puede faltar)."
                                + " En runtime el NPE es silencioso: log y campo vacío en el PDF."));
                    }
                }
            }
        }

        Violacion.assertNone("[P2] En las expresiones Groovy de los documentos PDF, toda navegación por una propiedad"
                + " que puede ser null (la que no es required=\"true\" en el modelo, o viene tras un ?.) debe usar ?.:"
                + " sin él, un null en mitad de la cadena es un NPE que en runtime deja el campo vacío en silencio.",
                violaciones);
    }
}
