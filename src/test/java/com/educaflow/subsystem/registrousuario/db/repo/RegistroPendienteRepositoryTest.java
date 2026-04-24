package com.educaflow.subsystem.registrousuario.db.repo;

import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.registrousuario.db.UsuarioAutorizado;
import com.educaflow.subsystem.security.db.TipoUsuario;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de resolución de perfiles de usuario por centro.
 *
 * El input representa lo que devuelve la query SQL: un registro por (centro, tipoUsuario)
 * con el curso más alto encontrado para ese par. La lógica Java decide entonces si ese
 * registro corresponde al curso actual del centro o a uno anterior.
 */
class RegistroPendienteRepositoryTest {

    // ── helpers ──────────────────────────────────────────────────────────────

    private Centro centro(Integer cursoActual) {
        Centro c = new Centro();
        c.setCurso(cursoActual);
        return c;
    }

    private TipoUsuario tipo(String code) {
        TipoUsuario t = new TipoUsuario();
        t.setCode(code);
        return t;
    }

    private UsuarioAutorizado entrada(Centro centro, TipoUsuario tipoUsuario, int cursoRegistro) {
        UsuarioAutorizado ua = new UsuarioAutorizado();
        ua.setCentro(centro);
        ua.setTipoUsuario(tipoUsuario);
        ua.setCurso(cursoRegistro);
        return ua;
    }

    /** Tipos precargados que simularían los disponibles en BD. */
    private Map<String, TipoUsuario> tipos(String... codigos) {
        Map<String, TipoUsuario> map = new java.util.LinkedHashMap<>();
        for (String code : codigos) map.put(code, tipo(code));
        return map;
    }

    private List<String> codigosEn(Map<Centro, List<TipoUsuario>> resultado, Centro centro) {
        return resultado.get(centro).stream()
                .map(TipoUsuario::getCode)
                .toList();
    }

    // ── profesor ─────────────────────────────────────────────────────────────

    @Test
    void profesorDelCursoActual_esProfesor() {
        Centro c = centro(2024);
        TipoUsuario profesor = tipo("PROFESOR");
        var entradas = List.of(entrada(c, profesor, 2024));
        var tipos = tipos("PROFESOR", "EXPROFESOR");

        var resultado = RegistroPendienteRepository.resolverPerfiles(entradas, tipos);

        assertEquals(List.of("PROFESOR"), codigosEn(resultado, c));
    }

    @Test
    void profesorDeCursoAnterior_esExprofesor() {
        Centro c = centro(2024);
        TipoUsuario profesor = tipo("PROFESOR");
        // La query ya devuelve solo el MAX curso: 2023
        var entradas = List.of(entrada(c, profesor, 2023));
        var tipos = tipos("PROFESOR", "EXPROFESOR");

        var resultado = RegistroPendienteRepository.resolverPerfiles(entradas, tipos);

        assertEquals(List.of("EXPROFESOR"), codigosEn(resultado, c));
    }

    @Test
    void profesorEnDosCentros_apareceEnAmbos() {
        Centro centroA = centro(2024);
        Centro centroB = centro(2024);
        TipoUsuario profesor = tipo("PROFESOR");
        var entradas = List.of(
                entrada(centroA, profesor, 2024),
                entrada(centroB, profesor, 2024)
        );
        var tipos = tipos("PROFESOR", "EXPROFESOR");

        var resultado = RegistroPendienteRepository.resolverPerfiles(entradas, tipos);

        assertEquals(2, resultado.size());
        assertEquals(List.of("PROFESOR"), codigosEn(resultado, centroA));
        assertEquals(List.of("PROFESOR"), codigosEn(resultado, centroB));
    }

    @Test
    void profesorActivoEnUnCentroYExEnOtro() {
        Centro centroActivo = centro(2024);
        Centro centroAntiguo = centro(2024);
        TipoUsuario profesor = tipo("PROFESOR");
        var entradas = List.of(
                entrada(centroActivo,  profesor, 2024),  // curso actual
                entrada(centroAntiguo, profesor, 2022)   // solo cursos anteriores
        );
        var tipos = tipos("PROFESOR", "EXPROFESOR");

        var resultado = RegistroPendienteRepository.resolverPerfiles(entradas, tipos);

        assertEquals(List.of("PROFESOR"),   codigosEn(resultado, centroActivo));
        assertEquals(List.of("EXPROFESOR"), codigosEn(resultado, centroAntiguo));
    }

    // ── alumno ───────────────────────────────────────────────────────────────

    @Test
    void alumnoDelCursoActual_esAlumno() {
        Centro c = centro(2024);
        TipoUsuario alumno = tipo("ALUMNO");
        var entradas = List.of(entrada(c, alumno, 2024));
        var tipos = tipos("ALUMNO", "EXALUMNO");

        var resultado = RegistroPendienteRepository.resolverPerfiles(entradas, tipos);

        assertEquals(List.of("ALUMNO"), codigosEn(resultado, c));
    }

    @Test
    void alumnoDeCursoAnterior_esExalumno() {
        Centro c = centro(2024);
        TipoUsuario alumno = tipo("ALUMNO");
        var entradas = List.of(entrada(c, alumno, 2021));
        var tipos = tipos("ALUMNO", "EXALUMNO");

        var resultado = RegistroPendienteRepository.resolverPerfiles(entradas, tipos);

        assertEquals(List.of("EXALUMNO"), codigosEn(resultado, c));
    }

    // ── familiar ─────────────────────────────────────────────────────────────

    @Test
    void familiarDelCursoActual_esFamiliar() {
        Centro c = centro(2024);
        TipoUsuario familiar = tipo("FAMILIAR");
        var entradas = List.of(entrada(c, familiar, 2024));
        var tipos = tipos("FAMILIAR");

        var resultado = RegistroPendienteRepository.resolverPerfiles(entradas, tipos);

        assertEquals(List.of("FAMILIAR"), codigosEn(resultado, c));
    }

    @Test
    void familiarDeCursoAnterior_esOmitido() {
        // FAMILIAR no tiene versión "ex" → si no es del curso actual no se incluye
        Centro c = centro(2024);
        TipoUsuario familiar = tipo("FAMILIAR");
        var entradas = List.of(entrada(c, familiar, 2022));
        var tipos = tipos("FAMILIAR");

        var resultado = RegistroPendienteRepository.resolverPerfiles(entradas, tipos);

        assertFalse(resultado.containsKey(c));
    }

    // ── combinaciones ────────────────────────────────────────────────────────

    @Test
    void profesorYAlumnoActivos_ambosenElMismoCentro() {
        Centro c = centro(2024);
        TipoUsuario profesor = tipo("PROFESOR");
        TipoUsuario alumno   = tipo("ALUMNO");
        var entradas = List.of(
                entrada(c, profesor, 2024),
                entrada(c, alumno,   2024)
        );
        var tipos = tipos("PROFESOR", "ALUMNO");

        var resultado = RegistroPendienteRepository.resolverPerfiles(entradas, tipos);

        assertThat(resultado.get(c)).extracting("code")
                .containsExactlyInAnyOrder("PROFESOR", "ALUMNO");
    }

    @Test
    void profesorActivoYExAlumnoMismoCentro() {
        Centro c = centro(2024);
        TipoUsuario profesor = tipo("PROFESOR");
        TipoUsuario alumno   = tipo("ALUMNO");
        var entradas = List.of(
                entrada(c, profesor, 2024),  // activo
                entrada(c, alumno,   2023)   // ex
        );
        var tipos = tipos("PROFESOR", "ALUMNO", "EXALUMNO");

        var resultado = RegistroPendienteRepository.resolverPerfiles(entradas, tipos);

        assertThat(resultado.get(c)).extracting("code")
                .containsExactlyInAnyOrder("PROFESOR", "EXALUMNO");
    }

    @Test
    void familiarYProfesorMismoCentro_familiarOmitidoSiAnterior() {
        Centro c = centro(2024);
        TipoUsuario familiar = tipo("FAMILIAR");
        TipoUsuario profesor = tipo("PROFESOR");
        var entradas = List.of(
                entrada(c, familiar, 2022),  // omitido
                entrada(c, profesor, 2024)   // activo
        );
        var tipos = tipos("FAMILIAR", "PROFESOR", "EXPROFESOR");

        var resultado = RegistroPendienteRepository.resolverPerfiles(entradas, tipos);

        assertEquals(List.of("PROFESOR"), codigosEn(resultado, c));
    }

    // ── casos límite ─────────────────────────────────────────────────────────

    @Test
    void centroSinCursoConfigurado_profesorEsExprofesor_familiarOmitido() {
        // centro.curso = null → nadie es del "curso actual"
        Centro c = centro(null);
        TipoUsuario profesor = tipo("PROFESOR");
        TipoUsuario familiar = tipo("FAMILIAR");
        var entradas = List.of(
                entrada(c, profesor, 2023),
                entrada(c, familiar, 2023)
        );
        var tipos = tipos("PROFESOR", "EXPROFESOR", "FAMILIAR");

        var resultado = RegistroPendienteRepository.resolverPerfiles(entradas, tipos);

        assertEquals(List.of("EXPROFESOR"), codigosEn(resultado, c));
    }

    @Test
    void sinEntradas_mapaVacio() {
        var resultado = RegistroPendienteRepository.resolverPerfiles(List.of(), Map.of());

        assertTrue(resultado.isEmpty());
    }

    // ── helper de assertThat sin dependencia de AssertJ ──────────────────────

    private ListAssertHelper assertThat(List<TipoUsuario> list) {
        return new ListAssertHelper(list);
    }

    private static class ListAssertHelper {
        private final List<TipoUsuario> list;
        ListAssertHelper(List<TipoUsuario> list) { this.list = list; }

        ListAssertHelper extracting(String field) { return this; }

        void containsExactlyInAnyOrder(String... expected) {
            List<String> actual = list.stream().map(TipoUsuario::getCode).toList();
            assertEquals(expected.length, actual.size(),
                    "Tamaño esperado " + expected.length + " pero fue " + actual.size() + ": " + actual);
            for (String code : expected) {
                assertTrue(actual.contains(code), "Esperado '" + code + "' en " + actual);
            }
        }
    }
}