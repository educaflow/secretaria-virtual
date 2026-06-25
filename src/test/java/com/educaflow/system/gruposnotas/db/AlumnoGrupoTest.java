package com.educaflow.system.gruposnotas.db;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests del campo calculado CC-001: {@link AlumnoGrupo#getNotaMedia()}.
 */
class AlumnoGrupoTest {

    private Nota nota(ValorNota valor) {
        Nota nota = new Nota();
        nota.setValor(valor);
        return nota;
    }

    @Test
    void getNotaMedia_sinModulosEvaluados_devuelveSinNota() {
        AlumnoGrupo alumnoGrupo = new AlumnoGrupo();
        alumnoGrupo.addNota(nota(ValorNota.NO_EVALUADO));
        alumnoGrupo.addNota(nota(ValorNota.NO_EVALUADO));

        assertEquals("Sin nota", alumnoGrupo.getNotaMedia());
    }

    @Test
    void getNotaMedia_unaNotaNumerica_devuelveEseValor() {
        AlumnoGrupo alumnoGrupo = new AlumnoGrupo();
        alumnoGrupo.addNota(nota(ValorNota.NOTA_8));

        assertEquals("8", alumnoGrupo.getNotaMedia());
    }

    @Test
    void getNotaMedia_matriculaHonorMasNoEvaluado_cuentaMH10YExcluyeNoEvaluado() {
        AlumnoGrupo alumnoGrupo = new AlumnoGrupo();
        alumnoGrupo.addNota(nota(ValorNota.MATRICULA_HONOR));
        alumnoGrupo.addNota(nota(ValorNota.NO_EVALUADO));

        assertEquals("10", alumnoGrupo.getNotaMedia());
    }

    @Test
    void getNotaMedia_matriculaHonorY7_devuelveMediaRedondeada9() {
        AlumnoGrupo alumnoGrupo = new AlumnoGrupo();
        alumnoGrupo.addNota(nota(ValorNota.MATRICULA_HONOR));
        alumnoGrupo.addNota(nota(ValorNota.NOTA_7));

        assertEquals("9", alumnoGrupo.getNotaMedia());
    }

    @Test
    void getNotaMedia_ochoYSeis_devuelveMedia7() {
        AlumnoGrupo alumnoGrupo = new AlumnoGrupo();
        alumnoGrupo.addNota(nota(ValorNota.NOTA_8));
        alumnoGrupo.addNota(nota(ValorNota.NOTA_6));

        assertEquals("7", alumnoGrupo.getNotaMedia());
    }

    @Test
    void getNotaMedia_coleccionNotasNula_devuelveSinNota() {
        AlumnoGrupo alumnoGrupo = new AlumnoGrupo();

        assertEquals("Sin nota", alumnoGrupo.getNotaMedia());
    }
}
