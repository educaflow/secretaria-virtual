package com.educaflow.base.infrastructure.pdfgenerator.impl.formulario.dibujo;

import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.dibujo.Alineacion;
import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.dibujo.Lienzo;
import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.expresion.Valores;
import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.modelo.TextoBilingue;
import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.texto.Fuente;
import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.texto.Maquetador;
import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.texto.Parrafo;
import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.texto.Token;
import com.educaflow.base.infrastructure.pdfgenerator.impl.formulario.maquetacion.CeldaUbicada;
import com.educaflow.base.infrastructure.pdfgenerator.impl.formulario.maquetacion.MedidasTabla;
import com.educaflow.base.infrastructure.pdfgenerator.impl.formulario.modelo.Celda;

import java.util.ArrayList;
import java.util.List;

import static com.educaflow.base.infrastructure.pdfgenerator.impl.comun.dibujo.MedidasPagina.CM;
import static com.educaflow.base.infrastructure.pdfgenerator.impl.formulario.maquetacion.MedidasTabla.CHECK_COL_W;
import static com.educaflow.base.infrastructure.pdfgenerator.impl.formulario.maquetacion.MedidasTabla.CHECK_LABEL_GAP;
import static com.educaflow.base.infrastructure.pdfgenerator.impl.formulario.maquetacion.MedidasTabla.CHECK_SIDE;
import static com.educaflow.base.infrastructure.pdfgenerator.impl.formulario.maquetacion.MedidasTabla.CTL_H;
import static com.educaflow.base.infrastructure.pdfgenerator.impl.formulario.maquetacion.MedidasTabla.EXTRA_H;
import static com.educaflow.base.infrastructure.pdfgenerator.impl.formulario.maquetacion.MedidasTabla.FULL;
import static com.educaflow.base.infrastructure.pdfgenerator.impl.formulario.maquetacion.MedidasTabla.LABEL_PAD_TOP;
import static com.educaflow.base.infrastructure.pdfgenerator.impl.formulario.maquetacion.MedidasTabla.LABEL_VACIO;
import static com.educaflow.base.infrastructure.pdfgenerator.impl.formulario.maquetacion.MedidasTabla.PAD;
import static com.educaflow.base.infrastructure.pdfgenerator.impl.formulario.maquetacion.MedidasTabla.PAD_VALOR;
import static com.educaflow.base.infrastructure.pdfgenerator.impl.formulario.maquetacion.MedidasTabla.ROW_CHECK;

/**
 * Una línea de la tabla (las celdas de una fila que caben en 12 columnas): primero se maqueta cada
 * celda para saber el alto de la línea, y luego se dibujan todas a esa altura.
 *
 * <p>Una celda reservada se maqueta igual (para que la línea mida lo mismo) pero solo se dibujan
 * sus bordes, y si lleva {@code campoFirma} no deja el campo de firma. Si la línea no llega a las 12 columnas (celdas colapsadas), el tramo que falta se
 * cierra con un recuadro vacío para que la tabla siga entera.
 */
final class DibujanteLinea {

    private static final double TAMANYO_ETIQUETA = 7;
    private static final double FACTOR_ALTO_TEXTO = 1.16;
    private static final double FACTOR_ALTO_ETIQUETA_CAMPO = 0.9;
    private static final double FACTOR_ALTO_VALOR = 1.15;
    /** Por debajo de este alto, en el hueco de una celda no cabe ni una línea de la firma. */
    private static final double ALTO_MINIMO_CAMPO_FIRMA = 0.5 * CM;

    private final Lienzo lienzo;
    private final Maquetador maquetador;
    private final Maquetador maquetadorReservado;
    private final Valores valores;

    /**
     * @param maquetador          el de las celdas que se dibujan: sus inline llevan el valor.
     * @param maquetadorReservado el de las celdas reservadas: sus inline quedan vacíos (no se evalúan).
     */
    DibujanteLinea(Lienzo lienzo, Maquetador maquetador, Maquetador maquetadorReservado, Valores valores) {
        this.lienzo = lienzo;
        this.maquetador = maquetador;
        this.maquetadorReservado = maquetadorReservado;
        this.valores = valores;
    }

    /** Una celda ya maquetada: sus párrafos y lo que necesita de alto. */
    private record CeldaMaquetada(CeldaUbicada ubicada, Parrafo etiqueta, Parrafo valenciano, Parrafo castellano,
                                  Parrafo valor, double altoControl, double gapIdiomas, double alto) {

        Celda celda() {
            return ubicada.celda();
        }

        double altoTextos() {
            return altoTextos(valenciano, castellano, gapIdiomas);
        }

        static double altoTextos(Parrafo valenciano, Parrafo castellano, double gapIdiomas) {
            boolean ambos = !valenciano.vacio() && !castellano.vacio();
            return valenciano.alto() + castellano.alto() + (ambos ? gapIdiomas : 0);
        }
    }

    void dibujar(List<CeldaUbicada> linea, boolean sinBordeSuperior, boolean sinBordeInferior) {
        List<CeldaMaquetada> celdas = new ArrayList<>();
        double altoLinea = 0;
        for (CeldaUbicada ubicada : linea) {
            CeldaMaquetada maquetada = maquetar(ubicada);
            celdas.add(maquetada);
            altoLinea = Math.max(altoLinea, maquetada.alto());
        }

        lienzo.asegurarEspacio(altoLinea);
        double top = lienzo.cursorY();
        for (CeldaMaquetada celda : celdas) {
            lienzo.bordesCelda(celda.ubicada().x(), top, celda.ubicada().ancho(), altoLinea, !sinBordeSuperior, !sinBordeInferior);
            if (celda.ubicada().reservada()) {
                continue;
            }
            switch (celda.celda().tipo()) {
                case CAMPO -> dibujarCampo(celda, top, altoLinea);
                case CHECK -> dibujarCheck(celda, top, altoLinea);
                case TEXTO -> dibujarTexto(celda, top, altoLinea);
            }
        }
        cerrarLineaIncompleta(linea, top, altoLinea, sinBordeSuperior, sinBordeInferior);
        lienzo.avanzar(altoLinea);
    }

    // ------------------------------------------------------------ maquetar

    private CeldaMaquetada maquetar(CeldaUbicada ubicada) {
        Maquetador maq = ubicada.reservada() ? maquetadorReservado : maquetador;
        return switch (ubicada.celda().tipo()) {
            case CAMPO -> maquetarCampo(ubicada, maq);
            case CHECK -> maquetarCheck(ubicada, maq);
            case TEXTO -> maquetarTexto(ubicada, maq);
        };
    }

    private CeldaMaquetada maquetarCampo(CeldaUbicada ubicada, Maquetador maq) {
        Celda celda = ubicada.celda();
        double anchoCelda = ubicada.ancho();
        double extra = (celda.rowSpan() - 1) * EXTRA_H;
        Parrafo valor = ubicada.reservada()
                ? Parrafo.VACIO
                : maq.parrafo(valores.texto(celda.nombreCampo().orElseThrow()),
                        MedidasTabla.ESTILO_VALOR.fuente(), MedidasTabla.ESTILO_VALOR.tamanyo(),
                        false, FACTOR_ALTO_VALOR, anchoValor(anchoCelda));
        // el hueco del valor crece si el valor necesita más líneas de las que caben
        double altoControl = Math.max(CTL_H + extra, valor.alto() + 2 * PAD_VALOR);
        Parrafo etiqueta = etiquetaCampo(celda.textos(), maq, anchoCelda - 2 * PAD);
        double alto = etiqueta.vacio()
                ? altoControl + 2 * LABEL_VACIO
                : LABEL_PAD_TOP + etiqueta.alto() + 0.03 * CM + altoControl + 0.03 * CM;
        return new CeldaMaquetada(ubicada, etiqueta, Parrafo.VACIO, Parrafo.VACIO, valor, altoControl, 0, alto);
    }

    /** La etiqueta de un campo: «VALENCIÀ / CASTELLANO», el castellano en cursiva, en una sola tirada. */
    private static Parrafo etiquetaCampo(TextoBilingue textos, Maquetador maq, double anchoMaximo) {
        if (textos.vacio()) {
            return Parrafo.VACIO;
        }
        List<Token> tokens = maq.tokenizar(textos.valenciano(), Fuente.REGULAR, TAMANYO_ETIQUETA, true);
        if (textos.tieneCastellano()) {
            maq.anyadirPalabras(tokens, textos.tieneValenciano() ? " / " : "", Fuente.REGULAR, TAMANYO_ETIQUETA, false);
            tokens.addAll(maq.tokenizar(textos.castellano(), Fuente.CURSIVA, TAMANYO_ETIQUETA, true));
        }
        return maq.parrafo(tokens, FACTOR_ALTO_ETIQUETA_CAMPO, anchoMaximo);
    }

    private CeldaMaquetada maquetarCheck(CeldaUbicada ubicada, Maquetador maq) {
        Celda celda = ubicada.celda();
        double anchoTexto = ubicada.ancho() - CHECK_COL_W - CHECK_LABEL_GAP - PAD;
        double extra = (celda.rowSpan() - 1) * EXTRA_H;
        Parrafo valenciano = maq.parrafo(celda.textos().valenciano(), Fuente.REGULAR, TAMANYO_ETIQUETA, false, FACTOR_ALTO_TEXTO, anchoTexto);
        Parrafo castellano = maq.parrafo(celda.textos().castellano(), Fuente.CURSIVA, TAMANYO_ETIQUETA, false, FACTOR_ALTO_TEXTO, anchoTexto);
        // separación amplia si lleva inline (para que sus huecos no se toquen), estrecha si es solo texto
        double gapIdiomas = (celda.textos().tieneInline() ? 0.2 : 0.06) * CM;
        double alto = Math.max(ROW_CHECK + extra, CeldaMaquetada.altoTextos(valenciano, castellano, gapIdiomas) + 2 * PAD);
        return new CeldaMaquetada(ubicada, Parrafo.VACIO, valenciano, castellano, Parrafo.VACIO, 0, gapIdiomas, alto);
    }

    private CeldaMaquetada maquetarTexto(CeldaUbicada ubicada, Maquetador maq) {
        Celda celda = ubicada.celda();
        double anchoTexto = ubicada.ancho() - 2 * PAD;
        Parrafo valenciano = maq.parrafo(celda.textos().valenciano(), Fuente.REGULAR, TAMANYO_ETIQUETA, false, FACTOR_ALTO_TEXTO, anchoTexto);
        Parrafo castellano = maq.parrafo(celda.textos().castellano(), Fuente.CURSIVA, TAMANYO_ETIQUETA, false, FACTOR_ALTO_TEXTO, anchoTexto);
        double minimo = celda.rowSpan() > 1 ? 0.5 * CM * celda.rowSpan() : 0;
        double alto = Math.max(valenciano.alto() + castellano.alto() + 2 * PAD, minimo);
        return new CeldaMaquetada(ubicada, Parrafo.VACIO, valenciano, castellano, Parrafo.VACIO, 0, 0, alto);
    }

    private static double anchoValor(double anchoCelda) {
        return anchoCelda - 0.28 * CM - 2 * PAD_VALOR;
    }

    // -------------------------------------------------------------- dibujar

    private void dibujarCampo(CeldaMaquetada celda, double top, double altoLinea) {
        double x = celda.ubicada().x();
        double anchoCelda = celda.ubicada().ancho();
        double baseControl;
        if (celda.etiqueta().vacio()) {
            baseControl = top - altoLinea + (altoLinea - celda.altoControl()) / 2;
        } else {
            lienzo.parrafo(celda.etiqueta(), x + PAD, top - LABEL_PAD_TOP, anchoCelda - 2 * PAD, Alineacion.IZQUIERDA);
            baseControl = top - altoLinea + 0.03 * CM;
        }
        // el valor, centrado verticalmente en su hueco
        double topValor = baseControl + celda.altoControl() - (celda.altoControl() - celda.valor().alto()) / 2;
        lienzo.parrafo(celda.valor(), x + PAD + PAD_VALOR, topValor, anchoValor(anchoCelda), Alineacion.IZQUIERDA);
    }

    private void dibujarCheck(CeldaMaquetada celda, double top, double altoLinea) {
        double x = celda.ubicada().x();
        boolean marcada = valores.marcada(celda.celda().nombreCampo().orElseThrow());
        lienzo.casilla(x + CHECK_COL_W - CHECK_SIDE, top - altoLinea / 2 - CHECK_SIDE / 2, CHECK_SIDE, marcada);
        if (celda.ubicada().unidades() <= 100) {
            return; // una casilla de una sola columna no tiene sitio para etiqueta
        }
        double xTexto = x + CHECK_COL_W + CHECK_LABEL_GAP;
        double anchoTexto = celda.ubicada().ancho() - CHECK_COL_W - CHECK_LABEL_GAP - PAD;
        double y = top - (altoLinea - celda.altoTextos()) / 2;
        lienzo.parrafo(celda.valenciano(), xTexto, y, anchoTexto, Alineacion.IZQUIERDA);
        if (!celda.valenciano().vacio()) {
            y -= celda.valenciano().alto() + celda.gapIdiomas();
        }
        lienzo.parrafo(celda.castellano(), xTexto, y, anchoTexto, Alineacion.IZQUIERDA);
    }

    private void dibujarTexto(CeldaMaquetada celda, double top, double altoLinea) {
        double x = celda.ubicada().x() + PAD;
        double anchoTexto = celda.ubicada().ancho() - 2 * PAD;
        double y = top - PAD;
        lienzo.parrafo(celda.valenciano(), x, y, anchoTexto, Alineacion.IZQUIERDA);
        y -= celda.valenciano().alto();
        lienzo.parrafo(celda.castellano(), x, y, anchoTexto, Alineacion.IZQUIERDA);
        y -= celda.castellano().alto();

        double base = top - altoLinea + PAD;
        double hueco = y - base;
        celda.celda().campoFirma().ifPresent(nombre -> dibujarCampoFirma(nombre, x, base, anchoTexto, hueco));
    }

    /** El campo de firma ocupa el hueco que queda en la celda por debajo de sus textos. */
    private void dibujarCampoFirma(String nombre, double x, double base, double ancho, double alto) {
        if (alto < ALTO_MINIMO_CAMPO_FIRMA) {
            throw new RuntimeException("En la celda del campoFirma \"" + nombre + "\" no queda hueco para la firma"
                    + " debajo de sus textos: dale más alto con rowSpan");
        }
        lienzo.campoFirma(nombre, x, base, ancho, alto);
    }

    private void cerrarLineaIncompleta(List<CeldaUbicada> linea, double top, double altoLinea,
                                       boolean sinBordeSuperior, boolean sinBordeInferior) {
        int fin = linea.get(linea.size() - 1).fin();
        if (fin < FULL) {
            lienzo.bordesCelda(MedidasTabla.x(fin), top, MedidasTabla.ancho(FULL - fin), altoLinea, !sinBordeSuperior, !sinBordeInferior);
        }
    }
}
