package com.educaflow.base.infrastructure.pdfgenerator.impl.formulario.dibujo;

import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.dibujo.Alineacion;
import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.dibujo.Lienzo;
import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.texto.Fuente;
import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.texto.Maquetador;
import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.texto.Parrafo;
import com.educaflow.base.infrastructure.pdfgenerator.impl.formulario.maquetacion.MedidasTabla;
import com.educaflow.base.infrastructure.pdfgenerator.impl.formulario.visibilidad.SeccionVisible;

import static com.educaflow.base.infrastructure.pdfgenerator.impl.comun.dibujo.MedidasPagina.CM;
import static com.educaflow.base.infrastructure.pdfgenerator.impl.formulario.maquetacion.MedidasTabla.FULL;
import static com.educaflow.base.infrastructure.pdfgenerator.impl.formulario.maquetacion.MedidasTabla.LETRA_EDGE;
import static com.educaflow.base.infrastructure.pdfgenerator.impl.formulario.maquetacion.MedidasTabla.PAD;
import static com.educaflow.base.infrastructure.pdfgenerator.impl.formulario.maquetacion.MedidasTabla.ROW_SECCION;
import static com.educaflow.base.infrastructure.pdfgenerator.impl.formulario.maquetacion.MedidasTabla.TABLE_W;

/**
 * La cabecera de una sección: la letra en su celda gris y el título bilingüe en mayúsculas. Una
 * sección reservada ocupa lo mismo pero solo lleva los bordes.
 */
final class DibujanteSeccion {

    private static final double TAMANYO = 9;
    private static final double FACTOR_ALTO = 0.85;
    private static final double TAMANYO_LETRA = 16;
    /** Aire entre el valenciano y el borde superior. */
    private static final double PAD_SUPERIOR = 0.12 * CM;
    /** Separación entre valenciano y castellano. PAD_SUPERIOR + GAP_IDIOMAS suman 0.14 cm: si se ajusta uno, compensar el otro. */
    private static final double GAP_IDIOMAS = 0.02 * CM;
    private static final double GRIS_LETRA = 0.6;

    private final Lienzo lienzo;
    private final Maquetador maquetador;
    private final Maquetador maquetadorReservado;

    /**
     * @param maquetador          el de las secciones que se dibujan: los inline del título llevan el valor.
     * @param maquetadorReservado el de las secciones reservadas: los inline del título quedan vacíos (no se evalúan).
     */
    DibujanteSeccion(Lienzo lienzo, Maquetador maquetador, Maquetador maquetadorReservado) {
        this.lienzo = lienzo;
        this.maquetador = maquetador;
        this.maquetadorReservado = maquetadorReservado;
    }

    void dibujarCabecera(SeccionVisible seccion, char letra) {
        double anchoTexto = MedidasTabla.ancho(FULL - LETRA_EDGE) - 2 * PAD;
        Maquetador maq = seccion.reservada() ? maquetadorReservado : maquetador;
        Parrafo valenciano = maq.parrafo(seccion.titulo().valenciano(), Fuente.SEMINEGRITA, TAMANYO, true, FACTOR_ALTO, anchoTexto);
        Parrafo castellano = maq.parrafo(seccion.titulo().castellano(), Fuente.SEMINEGRITA_CURSIVA, TAMANYO, true, FACTOR_ALTO, anchoTexto);
        boolean ambos = !valenciano.vacio() && !castellano.vacio();
        double altoTexto = valenciano.alto() + castellano.alto() + (ambos ? GAP_IDIOMAS : 0);
        double alto = Math.max(ROW_SECCION, PAD_SUPERIOR + altoTexto + 0.049 * CM);

        lienzo.asegurarEspacio(alto);
        double top = lienzo.cursorY();
        double anchoLetra = MedidasTabla.ancho(LETRA_EDGE);
        if (!seccion.reservada()) {
            lienzo.rellenarGris(MedidasTabla.x(0), top - alto, anchoLetra, alto, GRIS_LETRA);
        }
        lienzo.bordesCelda(MedidasTabla.x(0), top, anchoLetra, alto, true, true);
        lienzo.bordesCelda(MedidasTabla.x(LETRA_EDGE), top, TABLE_W - anchoLetra, alto, true, true);

        if (!seccion.reservada()) {
            dibujarLetra(letra, top, alto, anchoLetra);
            double y = top - PAD_SUPERIOR;
            lienzo.parrafo(valenciano, MedidasTabla.x(LETRA_EDGE) + PAD, y, anchoTexto, Alineacion.IZQUIERDA);
            if (!valenciano.vacio()) {
                y -= valenciano.alto() + GAP_IDIOMAS;
            }
            lienzo.parrafo(castellano, MedidasTabla.x(LETRA_EDGE) + PAD, y, anchoTexto, Alineacion.IZQUIERDA);
        }
        lienzo.avanzar(alto);
    }

    private void dibujarLetra(char letra, double top, double alto, double anchoLetra) {
        String texto = String.valueOf(letra);
        double anchoTexto = maquetador.tokenizar(texto, Fuente.REGULAR, TAMANYO_LETRA, false).get(0).ancho();
        lienzo.texto(Fuente.REGULAR, TAMANYO_LETRA, MedidasTabla.x(0) + (anchoLetra - anchoTexto) / 2,
                top - alto + (alto - TAMANYO_LETRA * 0.72) / 2, texto);
    }
}
