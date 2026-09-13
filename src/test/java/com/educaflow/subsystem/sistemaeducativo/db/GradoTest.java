package com.educaflow.subsystem.sistemaeducativo.db;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * CC-Grado-001 — el getter {@code getAdmiteNivel()} recalcula en cada lectura si el grado
 * tiene al menos un nivel NO archivado. El cuerpo del cálculo vive en
 * {@code domains/Grado.xml} y el generador de AOP lo emite como {@code computeAdmiteNivel()}.
 *
 * <p>El getter generado captura y registra la {@code NullPointerException} del cálculo y
 * devuelve el último valor del campo de respaldo, que nace a {@code Boolean.FALSE}; por eso
 * los casos que esperan {@code FALSE} siembran antes el campo a {@code Boolean.TRUE}: así el
 * {@code FALSE} observado solo puede venir de un {@code computeAdmiteNivel()} ejecutado con
 * éxito, y no de que el cálculo no llegue a ejecutarse.
 */
class GradoTest {

    private static final Boolean ARCHIVADO = Boolean.TRUE;
    private static final Boolean NO_ARCHIVADO = Boolean.FALSE;

    /* ------------------------------------------------------------------ */
    /* Helpers                                                            */
    /* ------------------------------------------------------------------ */

    private static Nivel nivel(Boolean archived) {
        Nivel nivel = new Nivel();
        nivel.setArchived(archived);
        return nivel;
    }

    /* ------------------------------------------------------------------ */
    /* getAdmiteNivel                                                     */
    /* ------------------------------------------------------------------ */

    @Test
    void getAdmiteNivel_sinNiveles_devuelveFalse() {
        Grado grado = new Grado();
        grado.setAdmiteNivel(Boolean.TRUE);

        assertEquals(false, grado.getAdmiteNivel());
    }

    @Test
    void getAdmiteNivel_listaDeNivelesVacia_devuelveFalse() {
        Grado grado = new Grado();
        grado.setAdmiteNivel(Boolean.TRUE);
        grado.setNiveles(new ArrayList<>());

        assertEquals(false, grado.getAdmiteNivel());
    }

    @Test
    void getAdmiteNivel_conUnNivelNoArchivado_devuelveTrue() {
        Grado grado = new Grado();
        grado.setNiveles(List.of(nivel(NO_ARCHIVADO)));

        assertEquals(true, grado.getAdmiteNivel());
    }

    @Test
    void getAdmiteNivel_conNivelSinArchivedInformado_devuelveTrue() {
        Grado grado = new Grado();
        // archived sin informar (null): cuenta como NO archivado.
        grado.setNiveles(List.of(nivel(null)));

        assertEquals(true, grado.getAdmiteNivel());
    }

    @Test
    void getAdmiteNivel_conTodosLosNivelesArchivados_devuelveFalse() {
        Grado grado = new Grado();
        grado.setAdmiteNivel(Boolean.TRUE);
        grado.setNiveles(List.of(nivel(ARCHIVADO), nivel(ARCHIVADO)));

        assertEquals(false, grado.getAdmiteNivel());
    }

    @Test
    void getAdmiteNivel_conNivelesArchivadosYUnoActivo_devuelveTrue() {
        Grado grado = new Grado();
        grado.setNiveles(List.of(nivel(ARCHIVADO), nivel(ARCHIVADO), nivel(NO_ARCHIVADO)));

        assertEquals(true, grado.getAdmiteNivel());
    }

    @Test
    void getAdmiteNivel_trasAnadirUnNivel_recalculaEnCadaLectura() {
        Grado grado = new Grado();
        grado.setAdmiteNivel(Boolean.TRUE);
        grado.setNiveles(new ArrayList<>());

        Boolean antes = grado.getAdmiteNivel();

        grado.setNiveles(List.of(nivel(NO_ARCHIVADO)));

        assertEquals(false, antes);
        assertEquals(true, grado.getAdmiteNivel());
    }
}
