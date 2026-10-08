package com.educaflow.base.infrastructure.pdfgenerator.impl.documentotexto.dibujo;

import com.educaflow.base.util.Idioma;
import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.dibujo.PdfDibujado;
import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.expresion.Valores;
import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.texto.Familia;
import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.texto.Maquetador;
import com.educaflow.base.infrastructure.pdfgenerator.impl.documentotexto.maquetacion.MaquetadorCuerpo;
import com.educaflow.base.infrastructure.pdfgenerator.impl.documentotexto.maquetacion.MedidasTexto;
import com.educaflow.base.infrastructure.pdfgenerator.impl.documentotexto.maquetacion.Renglon;
import com.educaflow.base.infrastructure.pdfgenerator.impl.documentotexto.visibilidad.DocumentoTextoVisible;

/**
 * Dibuja un {@link DocumentoTextoVisible} con sus {@link Valores}: la cabecera con el logo y el
 * título y, debajo, el cuerpo soltando renglón a renglón. Cuando el siguiente no cabe se abre
 * página nueva, así que un párrafo o una lista sí se parten entre páginas y una fila no.
 *
 * <p>La cabecera va dentro del flujo, así que <b>solo sale en la primera página</b>; las siguientes
 * son cuerpo de arriba abajo.
 */
public final class DibujanteDocumentoTexto {

    private DibujanteDocumentoTexto() {
    }

    public static byte[] dibujar(DocumentoTextoVisible documento, Valores valores, Idioma idioma) {
        return PdfDibujado.generar(Familia.MONTSERRAT, (lienzo, fuentes) -> {
            Maquetador conValores = new Maquetador(fuentes, valores::texto, MedidasTexto.ESTILO_VALOR);
            MaquetadorCuerpo maquetador = new MaquetadorCuerpo(conValores,
                    new Maquetador(fuentes, expresion -> "", MedidasTexto.ESTILO_VALOR),
                    idioma);

            CabeceraDocumentoTexto.dibujar(lienzo, conValores, documento.titulo(), idioma);

            for (Renglon renglon : maquetador.renglones(documento.cuerpo())) {
                lienzo.asegurarEspacio(renglon.alto());
                renglon.dibujo().accept(lienzo, lienzo.cursorY());
                lienzo.avanzar(renglon.alto());
            }
        });
    }
}
