package com.educaflow.base.infrastructure.pdfgenerator.impl.documentotexto.maquetacion;

import com.educaflow.base.infrastructure.pdfgenerator.Idioma;
import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.dibujo.Alineacion;
import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.dibujo.Lienzo;
import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.texto.Fuente;
import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.texto.Linea;
import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.texto.Maquetador;
import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.texto.Parrafo;
import com.educaflow.base.infrastructure.pdfgenerator.impl.documentotexto.modelo.Espacio;
import com.educaflow.base.infrastructure.pdfgenerator.impl.documentotexto.modelo.Fila;
import com.educaflow.base.infrastructure.pdfgenerator.impl.documentotexto.modelo.Lista;
import com.educaflow.base.infrastructure.pdfgenerator.impl.documentotexto.modelo.Tabla;
import com.educaflow.base.infrastructure.pdfgenerator.impl.documentotexto.modelo.Texto;
import com.educaflow.base.infrastructure.pdfgenerator.impl.documentotexto.visibilidad.ElementoVisible;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static com.educaflow.base.infrastructure.pdfgenerator.impl.documentotexto.maquetacion.MedidasTexto.ANCHO_CUERPO;
import static com.educaflow.base.infrastructure.pdfgenerator.impl.documentotexto.maquetacion.MedidasTexto.DESCENSO_VINYETA;
import static com.educaflow.base.infrastructure.pdfgenerator.impl.documentotexto.maquetacion.MedidasTexto.FACTOR_ALTO;
import static com.educaflow.base.infrastructure.pdfgenerator.impl.documentotexto.maquetacion.MedidasTexto.MARGEN_IZQUIERDO;
import static com.educaflow.base.infrastructure.pdfgenerator.impl.documentotexto.maquetacion.MedidasTexto.SANGRADO_ITEM;
import static com.educaflow.base.infrastructure.pdfgenerator.impl.documentotexto.maquetacion.MedidasTexto.SANGRADO_VINYETA;
import static com.educaflow.base.infrastructure.pdfgenerator.impl.documentotexto.maquetacion.MedidasTexto.TAMANYO;
import static com.educaflow.base.infrastructure.pdfgenerator.impl.documentotexto.maquetacion.MedidasTexto.TAMANYO_VINYETA;
import static com.educaflow.base.infrastructure.pdfgenerator.impl.documentotexto.maquetacion.MedidasTexto.VINYETA;

/**
 * Convierte el cuerpo del documento en la lista de renglones que hay que ir soltando por la página.
 * Un elemento reservado se mide igual que si se dibujara —con los inline vacíos, que no se han
 * evaluado— y sus renglones se quedan sin dibujo (ni campo de firma, si lo llevaba).
 */
public final class MaquetadorCuerpo {

    private final Maquetador maquetador;
    private final Maquetador maquetadorReservado;
    private final Idioma idioma;
    private final Parrafo vinyeta;

    /**
     * @param maquetador          el de lo que se dibuja: sus inline llevan el valor.
     * @param maquetadorReservado el de lo reservado: sus inline quedan vacíos (no se evalúan).
     */
    public MaquetadorCuerpo(Maquetador maquetador, Maquetador maquetadorReservado, Idioma idioma) {
        this.maquetador = maquetador;
        this.maquetadorReservado = maquetadorReservado;
        this.idioma = idioma;
        this.vinyeta = vinyeta(maquetador);
    }

    /** La viñeta es mayor que su texto pero comparte con él el alto de línea, del que sale la línea base. */
    private static Parrafo vinyeta(Maquetador maquetador) {
        double factorAlto = TAMANYO * FACTOR_ALTO / TAMANYO_VINYETA;
        return maquetador.parrafo(VINYETA, Fuente.REGULAR, TAMANYO_VINYETA, false, factorAlto, SANGRADO_ITEM);
    }

    public List<Renglon> renglones(List<ElementoVisible> cuerpo) {
        return cuerpo.stream()
                .flatMap(bloque -> renglones(bloque, MARGEN_IZQUIERDO, ANCHO_CUERPO).stream())
                .toList();
    }

    private List<Renglon> renglones(ElementoVisible visible, double x, double ancho) {
        List<Renglon> renglones = renglonesDe(visible, x, ancho);
        if (!visible.reservado()) {
            return renglones;
        }
        return renglones.stream().map(Renglon::sinDibujo).toList();
    }

    private List<Renglon> renglonesDe(ElementoVisible visible, double x, double ancho) {
        return switch (visible.elemento()) {
            case Texto texto -> deTexto(texto, maquetadorDe(visible), x, ancho);
            case Lista ignorada -> deLista(visible, x, ancho);
            case Espacio espacio -> List.of(deEspacio(espacio, x, ancho));
            case Tabla tabla -> deTabla(tabla, visible, x, ancho);
            case Fila ignorada -> throw new IllegalStateException("Una <fila> solo se maqueta dentro de su <tabla>");
        };
    }

    private Maquetador maquetadorDe(ElementoVisible visible) {
        return visible.reservado() ? maquetadorReservado : maquetador;
    }

    /** Un espacio no dibuja nada, pero si lleva {@code campoFirma} deja el campo de firma en todo su hueco. */
    private static Renglon deEspacio(Espacio espacio, double x, double ancho) {
        return espacio.campoFirma()
                .map(nombre -> new Renglon(espacio.alto(),
                        (lienzo, top) -> lienzo.campoFirma(nombre, x, top - espacio.alto(), ancho, espacio.alto())))
                .orElseGet(() -> Renglon.enBlanco(espacio.alto()));
    }

    // ------------------------------------------------------------ texto y listas

    private List<Renglon> deTexto(Texto texto, Maquetador maquetador, double x, double ancho) {
        Parrafo parrafo = maquetador.parrafo(texto.textos().en(idioma), texto.fuente(),
                TAMANYO, texto.mayusculas(), FACTOR_ALTO, ancho);
        return lineaALinea(parrafo, x, ancho, texto.alineacion());
    }

    private static List<Renglon> lineaALinea(Parrafo parrafo, double x, double ancho, Alineacion alineacion) {
        return parrafo.lineas().stream()
                .map(linea -> new Renglon(parrafo.altoLinea(),
                        (lienzo, top) -> lienzo.parrafo(sola(linea, parrafo.altoLinea()), x, top, ancho, alineacion)))
                .toList();
    }

    private static Parrafo sola(Linea linea, double altoLinea) {
        return new Parrafo(List.of(linea), altoLinea);
    }

    private List<Renglon> deLista(ElementoVisible lista, double x, double ancho) {
        return lista.hijos().stream().flatMap(item -> deItem(item, x, ancho).stream()).toList();
    }

    /** El item va sangrado y su primer renglón lleva además la viñeta, que no se sangra con él. */
    private List<Renglon> deItem(ElementoVisible item, double x, double ancho) {
        List<Renglon> renglones = renglones(item, x + SANGRADO_ITEM, ancho - SANGRADO_ITEM);
        if (renglones.isEmpty() || item.reservado()) {
            return renglones;
        }
        Renglon primero = renglones.get(0);
        Renglon conVinyeta = new Renglon(primero.alto(), (lienzo, top) -> {
            lienzo.parrafo(vinyeta, x + SANGRADO_VINYETA, top - DESCENSO_VINYETA,
                    SANGRADO_ITEM - SANGRADO_VINYETA, Alineacion.IZQUIERDA);
            primero.dibujo().accept(lienzo, top);
        });
        return Stream.concat(Stream.of(conVinyeta), renglones.stream().skip(1)).toList();
    }

    // ------------------------------------------------------------------- tablas

    private List<Renglon> deTabla(Tabla tabla, ElementoVisible visible, double x, double ancho) {
        double anchoColumna = ancho / tabla.columnas();
        return visible.hijos().stream().map(fila -> deFila(fila, x, anchoColumna)).toList();
    }

    /** La fila entera es un solo renglón: mide lo que su celda más alta y no se parte entre páginas. */
    private Renglon deFila(ElementoVisible fila, double x, double anchoColumna) {
        List<List<Renglon>> celdas = new ArrayList<>();
        for (int columna = 0; columna < fila.hijos().size(); columna++) {
            celdas.add(renglones(fila.hijos().get(columna), x + columna * anchoColumna, anchoColumna));
        }
        double alto = celdas.stream().mapToDouble(MaquetadorCuerpo::alto).max().orElse(0);
        return new Renglon(alto, (lienzo, top) -> celdas.forEach(celda -> apilar(celda, lienzo, top)));
    }

    private static double alto(List<Renglon> renglones) {
        return renglones.stream().mapToDouble(Renglon::alto).sum();
    }

    private static void apilar(List<Renglon> renglones, Lienzo lienzo, double top) {
        double y = top;
        for (Renglon renglon : renglones) {
            renglon.dibujo().accept(lienzo, y);
            y -= renglon.alto();
        }
    }
}
