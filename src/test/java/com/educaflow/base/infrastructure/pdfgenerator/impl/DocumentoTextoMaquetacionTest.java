package com.educaflow.base.infrastructure.pdfgenerator.impl;

import com.educaflow.base.infrastructure.pdfgenerator.Idioma;
import com.educaflow.base.infrastructure.pdfgenerator.impl.documentotexto.maquetacion.MedidasTexto;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Cómo coloca el documento en prosa cada cosa dentro del ancho del cuerpo. */
class DocumentoTextoMaquetacionTest {

    // Las medidas se toman del propio generador: el test comprueba cómo se coloca cada cosa respecto
    // a los bordes del cuerpo, no cuánto vale cada medida, así que cambiar una no lo rompe.
    private static final double MARGEN_IZQUIERDO = MedidasTexto.MARGEN_IZQUIERDO;
    private static final double BORDE_DERECHO = MedidasTexto.BORDE_DERECHO;
    private static final double ANCHO_CUERPO = MedidasTexto.ANCHO_CUERPO;
    private static final double ALTO_LINEA = MedidasTexto.TAMANYO * MedidasTexto.FACTOR_ALTO;

    @Test
    void lasTresAlineacionesDeUnaLineaLaColocanRespectoDeLosDosBordes() {
        List<TextosDelPdf.Linea> lineas = cuerpo(DocumentosDeTexto.generar("documento_alineamientos.xml"));
        TextosDelPdf.Linea izquierda = lineas.get(0);
        TextosDelPdf.Linea centrado = lineas.get(1);
        TextosDelPdf.Linea derecha = lineas.get(2);

        assertEquals(izquierda.texto(), centrado.texto(), "las tres primeras líneas deben decir lo mismo para poder comparar su sitio");
        assertEquals(izquierda.texto(), derecha.texto(), "las tres primeras líneas deben decir lo mismo para poder comparar su sitio");

        assertEquals(MARGEN_IZQUIERDO, izquierda.xInicio(), 0.01, "el izquierda no arranca en el margen izquierdo");
        assertEquals(BORDE_DERECHO, derecha.xFin(), 0.01, "el derecha no termina en el borde derecho");
        assertEquals((izquierda.xInicio() + derecha.xInicio()) / 2, centrado.xInicio(), 0.01,
                "el centrado no deja el mismo aire a cada lado: no está a mitad de camino entre el izquierda y el derecha");
    }

    @Test
    void elJustificadoEstiraTodasSusLineasHastaElBordeDerechoMenosLaUltima() {
        List<TextosDelPdf.Linea> lineas = cuerpo(DocumentosDeTexto.generar("documento_alineamientos.xml"));
        List<TextosDelPdf.Linea> justificado = lineas.subList(3, lineas.size());

        assertTrue(justificado.size() >= 3, "el párrafo justificado debe ocupar varias líneas y ocupa " + justificado.size());
        justificado.subList(0, justificado.size() - 1).forEach(linea ->
                assertEquals(BORDE_DERECHO, linea.xFin(), 0.01,
                        "una línea interior del justificado no llega al borde derecho: «" + linea.texto() + "»"));

        TextosDelPdf.Linea ultima = justificado.get(justificado.size() - 1);
        assertTrue(ultima.xFin() < BORDE_DERECHO - 10,
                "la última línea del justificado se ha estirado hasta el borde: «" + ultima.texto() + "»");
    }

    @Test
    void elSaltoDeLineaAbreLineaNuevaYNoJustificaLaQueCierra() {
        List<TextosDelPdf.Linea> lineas = cuerpo(DocumentosDeTexto.generar("documento_salto_de_linea.xml"));

        assertEquals(3, lineas.size(), "el párrafo debe salir en tres líneas: dos antes del salto y una después");
        assertEquals(BORDE_DERECHO, lineas.get(0).xFin(), 0.01, "la primera línea es interior y debería justificarse");
        assertTrue(lineas.get(1).texto().endsWith("AQUI"), "la segunda línea no acaba donde acaba la primera mitad: «" + lineas.get(1).texto() + "»");
        assertTrue(lineas.get(2).texto().startsWith("DESPUES"), "la tercera línea no empieza donde empieza la segunda mitad: «" + lineas.get(2).texto() + "»");
        assertTrue(lineas.get(1).xFin() < BORDE_DERECHO - 10,
                "la línea que cierra el salto se ha justificado: «" + lineas.get(1).texto() + "»");
        assertEquals(ALTO_LINEA, lineas.get(1).y() - lineas.get(2).y(), 0.01,
                "las dos mitades no van en líneas consecutivas separadas un interlineado");
    }

    @Test
    void elValorDeUnaVariableSeEstampaEnLineaConElTextoQueLaRodea() {
        TextosDelPdf corto = variable("Ana");
        TextosDelPdf largo = variable("Ana Maria Gonzalez Lorenzo");

        assertFalse(corto.contiene("${self.nombre}"), "la expresión se estampó sin evaluar: " + corto.volcado());
        assertEquals("Antes Ana despues.", cuerpo(corto).get(0).texto(), "el valor no ocupa el sitio de su expresión");
        assertEquals("Antes Ana Maria Gonzalez Lorenzo despues.", cuerpo(largo).get(0).texto(),
                "el valor no ocupa el sitio de su expresión");
        assertEquals(corto.yDe("despues."), largo.yDe("despues."), 0.01, "el valor largo se llevó a otra línea lo que le sigue");
        assertTrue(largo.xDe("despues.") > corto.xDe("despues."),
                "el valor no empuja hacia la derecha lo que le sigue: no ocupa lo que mide");
    }

    @Test
    void unEspacioDejaExactamenteLosPuntosDeAltoQuePide() {
        TextosDelPdf textos = DocumentosDeTexto.generar("documento_espacios.xml");

        assertEquals(ALTO_LINEA + 40, textos.yDe("Arriba") - textos.yDe("Medio"), 0.01,
                "el espacio de 40 puntos no ha dejado 40 puntos");
        assertEquals(ALTO_LINEA + 10, textos.yDe("Medio") - textos.yDe("Abajo"), 0.01,
                "el espacio de 10 puntos no ha dejado 10 puntos");
    }

    @Test
    void laTablaAlineaSusColumnasYCadaFilaMideLoQueSuCeldaMasAlta() {
        TextosDelPdf textos = DocumentosDeTexto.generar("documento_tabla_de_firmas.xml");

        assertEquals(textos.yDe("Alta"), textos.yDe("Baja"), 0.01, "las dos celdas de la fila no arrancan a la misma altura");
        assertEquals(ANCHO_CUERPO / 2, textos.xDe("Baja") - textos.xDe("Alta"), 0.01,
                "las dos columnas no se reparten a partes iguales el ancho del cuerpo");
        assertEquals(3 * ALTO_LINEA, textos.yDe("Alta") - textos.yDe("Segunda"), 0.01,
                "la fila no mide las tres líneas de su celda más alta: " + textos.volcado());
        assertEquals(textos.yDe("Segunda"), textos.yDe("Ultima"), 0.01, "la segunda fila no va a una sola altura");
        assertEquals(0, textos.trazos(), "la tabla de un documento en prosa no lleva bordes y se ha trazado algo");
    }

    @Test
    void lasMayusculasAlcanzanTambienAlValorDeUnaVariable() {
        List<TextosDelPdf.Linea> lineas = negritaYMayusculas();

        assertEquals("Resolucion Ana final", lineas.get(0).texto(), "un párrafo sin mayusculas ha cambiado de caja");
        assertEquals("RESOLUCION ANA FINAL", lineas.get(1).texto(),
                "el mismo párrafo con mayusculas no ha subido de caja el texto y el valor de su variable");
    }

    @Test
    void laNegritaEstampaElMismoTextoConUnaFuenteMasAncha() {
        List<TextosDelPdf.Linea> lineas = negritaYMayusculas();
        TextosDelPdf.Linea redonda = lineas.get(2);
        TextosDelPdf.Linea negrita = lineas.get(3);

        assertEquals(redonda.texto(), negrita.texto(), "las dos líneas deben decir lo mismo para poder comparar su ancho");
        assertEquals(MARGEN_IZQUIERDO, negrita.xInicio(), 0.01, "la negrita no arranca en el margen izquierdo");
        assertTrue(negrita.xFin() > redonda.xFin(),
                "la negrita no ocupa más que la redonda: se ha estampado con la misma fuente");
    }

    // ------------------------------------------------------------------ apoyo

    private static List<TextosDelPdf.Linea> negritaYMayusculas() {
        return cuerpo(TextosDelPdf.de(DocumentosDeTexto.pdf("documento_negrita_y_mayusculas.xml",
                Map.of("nombre", "Ana"), Idioma.CASTELLANO)));
    }

    /**
     * Las líneas del cuerpo: sin la primera, que es el título de la cabecera y no se maqueta con el
     * ancho ni el margen del cuerpo.
     */
    private static List<TextosDelPdf.Linea> cuerpo(TextosDelPdf textos) {
        List<TextosDelPdf.Linea> lineas = textos.lineas();
        return lineas.subList(1, lineas.size());
    }

    private static TextosDelPdf variable(String nombre) {
        return TextosDelPdf.de(DocumentosDeTexto.pdf("documento_variable.xml", Map.of("nombre", nombre), Idioma.CASTELLANO));
    }
}
