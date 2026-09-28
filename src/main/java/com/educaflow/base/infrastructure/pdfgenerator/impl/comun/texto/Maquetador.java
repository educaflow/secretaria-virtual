package com.educaflow.base.infrastructure.pdfgenerator.impl.comun.texto;

import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.modelo.ExpresionInline;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.regex.Matcher;

/**
 * Trocea un texto en tokens (palabras, espacios, saltos duros y valores de los {@code ${expresion}})
 * y los ajusta a líneas de un ancho máximo con un ajuste voraz.
 *
 * <p>El valor de un inline lo da {@code valorInline}: el texto ya evaluado, o la cadena vacía para
 * las celdas reservadas, cuyas expresiones no se evalúan. {@code estiloValorInline} es la fuente y
 * el tamaño con los que se estampa ese valor, que los pone cada tipo de documento.
 */
public final class Maquetador {

    private static final Locale ES = Locale.of("es", "ES");

    private final MedidorTexto medidor;
    private final Function<String, String> valorInline;
    private final EstiloTexto estiloValorInline;

    public Maquetador(MedidorTexto medidor, Function<String, String> valorInline, EstiloTexto estiloValorInline) {
        this.medidor = medidor;
        this.valorInline = valorInline;
        this.estiloValorInline = estiloValorInline;
    }

    /** El párrafo de un texto; {@link Parrafo#VACIO} si el texto está vacío. */
    public Parrafo parrafo(String texto, Fuente fuente, double tamanyo, boolean mayusculas,
                           double factorAlto, double anchoMaximo) {
        if (texto.isEmpty()) {
            return Parrafo.VACIO;
        }
        return parrafo(tokenizar(texto, fuente, tamanyo, mayusculas), factorAlto, anchoMaximo);
    }

    /** El párrafo de unos tokens ya troceados (una etiqueta compuesta de varios textos y fuentes). */
    public Parrafo parrafo(List<Token> tokens, double factorAlto, double anchoMaximo) {
        if (tokens.isEmpty()) {
            return Parrafo.VACIO;
        }
        double tamanyoMayor = tokens.stream().mapToDouble(Token::tamanyo).max().orElse(0);
        return new Parrafo(ajustar(tokens, anchoMaximo), tamanyoMayor * factorAlto);
    }

    public List<Token> tokenizar(String texto, Fuente fuente, double tamanyo, boolean mayusculas) {
        List<Token> tokens = new ArrayList<>();
        Matcher m = ExpresionInline.PATRON.matcher(texto);
        int ultimo = 0;
        while (m.find()) {
            anyadirPalabras(tokens, texto.substring(ultimo, m.start()), fuente, tamanyo, mayusculas);
            anyadirValor(tokens, m.group(1), mayusculas);
            ultimo = m.end();
        }
        anyadirPalabras(tokens, texto.substring(ultimo), fuente, tamanyo, mayusculas);
        return tokens;
    }

    public void anyadirPalabras(List<Token> tokens, String texto, Fuente fuente, double tamanyo, boolean mayusculas) {
        if (mayusculas) {
            texto = texto.toUpperCase(ES);
        }
        for (String parte : partes(texto)) {
            if (parte.isEmpty()) {
                continue;
            }
            if (parte.equals("\n")) {
                tokens.add(new Token(TipoToken.SALTO, "", fuente, tamanyo, 0));
                continue;
            }
            TipoToken tipo = parte.equals(" ") ? TipoToken.ESPACIO : TipoToken.PALABRA;
            tokens.add(new Token(tipo, parte, fuente, tamanyo, medidor.ancho(fuente, parte, tamanyo)));
        }
    }

    /** Parte un texto en palabras, espacios y saltos, conservando cada separador como parte propia. */
    private static List<String> partes(String texto) {
        List<String> partes = new ArrayList<>();
        StringBuilder palabra = new StringBuilder();
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            if (c != ' ' && c != '\n') {
                palabra.append(c);
                continue;
            }
            if (!palabra.isEmpty()) {
                partes.add(palabra.toString());
                palabra.setLength(0);
            }
            partes.add(String.valueOf(c));
        }
        if (!palabra.isEmpty()) {
            partes.add(palabra.toString());
        }
        return partes;
    }

    /**
     * El valor de un inline se trocea en palabras, como cualquier otro texto: un valor largo
     * (el motivo de un rechazo, una observación) tiene que poder partirse en varias líneas. Si
     * fuese un token único, no cabría en la línea y se saldría del documento sin avisar.
     *
     * <p>Hereda las mayúsculas del texto que lo contiene —una línea en mayúsculas lo está entera—,
     * pero no su fuente: el valor se estampa siempre con {@code estiloValorInline}.
     */
    private void anyadirValor(List<Token> tokens, String expresion, boolean mayusculas) {
        anyadirPalabras(tokens, valorInline.apply(expresion),
                estiloValorInline.fuente(), estiloValorInline.tamanyo(), mayusculas);
    }

    /** Ajuste voraz de tokens a líneas de ancho máximo: un espacio nunca abre línea ni la desborda. */
    static List<Linea> ajustar(List<Token> tokens, double anchoMaximo) {
        List<Linea> lineas = new ArrayList<>();
        List<TokenColocado> linea = new ArrayList<>();
        double ancho = 0;
        for (Token token : tokens) {
            if (token.tipo() == TipoToken.SALTO) {
                lineas.add(new Linea(List.copyOf(linea), ancho, true));
                linea = new ArrayList<>();
                ancho = 0;
                continue;
            }
            if (token.tipo() == TipoToken.ESPACIO && linea.isEmpty()) {
                continue;
            }
            if (ancho + token.ancho() > anchoMaximo + 0.1 && !linea.isEmpty() && token.tipo() != TipoToken.ESPACIO) {
                lineas.add(new Linea(List.copyOf(linea), ancho, false));
                linea = new ArrayList<>();
                ancho = 0;
            }
            linea.add(new TokenColocado(token, ancho));
            ancho += token.ancho();
        }
        if (!linea.isEmpty() || lineas.isEmpty()) {
            lineas.add(new Linea(List.copyOf(linea), ancho, true));
        }
        return lineas;
    }
}
