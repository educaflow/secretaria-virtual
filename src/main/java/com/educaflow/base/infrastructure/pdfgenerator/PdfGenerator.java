package com.educaflow.base.infrastructure.pdfgenerator;

import java.util.List;
import java.util.Map;

/**
 * Genera el PDF de un documento a partir de su XML de definición <b>resuelto</b> (el que deja el
 * build en el classpath: includes expandidos, título puesto y valenciano traducido).
 *
 * <p>El PDF sale <b>plano</b>: las expresiones Groovy del XML ({@code nombreCampo}, los
 * {@code ${expresion}} inline y los {@code visible}) se evalúan con el contexto y los valores y las
 * casillas se dibujan directamente, sin formulario. Un elemento cuyo {@code visible} evalúa a
 * {@code false} se colapsa (desaparece y lo que va detrás sube) o reserva su hueco, según su
 * {@code siOculto}.
 *
 * <p>Toda expresión que no evalúe, todo {@code visible} que no devuelva {@code Boolean} y toda
 * casilla cuyo valor no sea {@code Boolean} abortan con {@link RuntimeException}: el documento no se
 * emite con un dato de menos.
 */
public interface PdfGenerator {

    /**
     * @param documentoXml el XML resuelto del documento (raíz {@code <documentoFormulario>} o
     *                     {@code <documentoTexto>}).
     * @param contexto     las variables de las expresiones Groovy (p.ej. {@code self} y {@code now}).
     * @param idioma       el idioma del documento. Un {@code <documentoFormulario>} lo <b>ignora</b>:
     *                     es bilingüe y estampa los dos idiomas. Lo usa {@code <documentoTexto>}, que
     *                     se emite en un solo idioma.
     * @return los bytes del PDF.
     */
    byte[] generate(byte[] documentoXml, Map<String, Object> contexto, Idioma idioma);

    /**
     * Todas las expresiones Groovy del documento ({@code nombreCampo}, los inline de ambos idiomas y
     * los {@code visible}), sin duplicados y en orden de aparición. Es lo que un test compila contra
     * la entidad del expediente antes de que ningún PDF se genere.
     */
    List<String> getExpresiones(byte[] documentoXml);
}
