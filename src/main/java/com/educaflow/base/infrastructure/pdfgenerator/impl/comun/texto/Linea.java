package com.educaflow.base.infrastructure.pdfgenerator.impl.comun.texto;

import java.util.List;
import java.util.stream.IntStream;

/**
 * Una línea de un párrafo ya ajustado: sus tokens colocados y el ancho total que ocupan.
 *
 * @param finalDeParrafo si cierra el párrafo o la cierra un salto duro, y por tanto no se justifica.
 */
public record Linea(List<TokenColocado> tokens, double ancho, boolean finalDeParrafo) {

    /** Los espacios que separan palabras: los que cierran la línea no la ensanchan. */
    public int espaciosInteriores() {
        return (int) IntStream.range(0, ultimoNoEspacio())
                .filter(i -> tokens.get(i).token().tipo() == TipoToken.ESPACIO)
                .count();
    }

    /** El ancho de la línea sin los espacios que la cierran. */
    public double anchoSinEspaciosFinales() {
        int ultimo = ultimoNoEspacio();
        if (ultimo < 0) {
            return 0;
        }
        TokenColocado colocado = tokens.get(ultimo);
        return colocado.x() + colocado.token().ancho();
    }

    private int ultimoNoEspacio() {
        for (int i = tokens.size() - 1; i >= 0; i--) {
            if (tokens.get(i).token().tipo() != TipoToken.ESPACIO) {
                return i;
            }
        }
        return -1;
    }
}
