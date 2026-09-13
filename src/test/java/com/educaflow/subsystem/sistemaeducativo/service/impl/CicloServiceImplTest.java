package com.educaflow.subsystem.sistemaeducativo.service.impl;

import com.axelor.db.modelservice.AllowProperties;
import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.i18n.I18n;
import com.educaflow.subsystem.sistemaeducativo.db.Ciclo;
import com.educaflow.subsystem.sistemaeducativo.db.FamiliaProfesional;
import com.educaflow.subsystem.sistemaeducativo.db.Grado;
import com.educaflow.subsystem.sistemaeducativo.db.Nivel;
import com.educaflow.subsystem.sistemaeducativo.db.repo.CicloRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class CicloServiceImplTest {

    private CicloRepository repository;
    private CicloServiceImpl service;

    private MockedStatic<I18n> i18nMock;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(CicloRepository.class);
        service = new CicloServiceImpl(Ciclo.class, repository);

        // lenient: I18n se programa en el setup pero los happy paths no producen mensaje y por
        // tanto no recorren las ramas que lo consumen.
        i18nMock = Mockito.mockStatic(I18n.class,
                Mockito.withSettings().strictness(Strictness.LENIENT));
        i18nMock.when(() -> I18n.get(any(String.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        i18nMock.close();
    }

    /* ------------------------------------------------------------------ */
    /* Helpers                                                            */
    /* ------------------------------------------------------------------ */

    private BusinessMessages mensajes(Optional<BusinessMessages> optional) {
        assertTrue(optional.isPresent());
        return optional.get();
    }

    private Nivel nivel(long id, Grado grado) {
        Nivel nivel = new Nivel();
        nivel.setId(id);
        nivel.setCode("N" + id);
        nivel.setName("Nivel " + id);
        nivel.setGrado(grado);
        return nivel;
    }

    /** Grado sin ningún nivel: {@code Grado.admiteNivel} calcula {@code false}. */
    private Grado gradoSinNiveles(long id) {
        Grado grado = new Grado();
        grado.setId(id);
        grado.setCode("G" + id);
        grado.setName("Grado " + id);
        grado.setNiveles(new ArrayList<>());
        return grado;
    }

    /** Grado con un nivel no archivado: {@code Grado.admiteNivel} calcula {@code true}. */
    private Grado gradoQueAdmiteNivel(long id) {
        Grado grado = gradoSinNiveles(id);
        grado.getNiveles().add(nivel(id * 1000, grado));
        return grado;
    }

    /** Grado cuyo único nivel está archivado: {@code Grado.admiteNivel} calcula {@code false}. */
    private Grado gradoConTodosSusNivelesArchivados(long id) {
        Grado grado = gradoSinNiveles(id);
        Nivel nivelArchivado = nivel(id * 1000, grado);
        nivelArchivado.setArchived(Boolean.TRUE);
        grado.getNiveles().add(nivelArchivado);
        return grado;
    }

    private Ciclo ciclo(Grado grado, Nivel nivel) {
        FamiliaProfesional familiaProfesional = new FamiliaProfesional();
        familiaProfesional.setId(1L);

        Ciclo ciclo = new Ciclo();
        ciclo.setCode("CFGS-DAM");
        ciclo.setName("Desarrollo de Aplicaciones Multiplataforma");
        ciclo.setFamiliaProfesional(familiaProfesional);
        ciclo.setGrado(grado);
        ciclo.setNivel(nivel);
        return ciclo;
    }

    /* ------------------------------------------------------------------ */
    /* validateInsert                                                     */
    /* ------------------------------------------------------------------ */

    @Test
    void validateInsert_gradoQueAdmiteNivelConNivelDeEseGrado_devuelveOptionalVacio() {
        Grado gradoCF = gradoQueAdmiteNivel(1L);
        Nivel nivelSuperior = nivel(10L, gradoCF);

        Optional<BusinessMessages> resultado = service.validateInsert(ciclo(gradoCF, nivelSuperior));

        assertTrue(resultado.isEmpty());
    }

    @Test
    void validateInsert_gradoQueAdmiteNivelSinNivel_devuelveMensajeNivelObligatorio() {
        Grado gradoCF = gradoQueAdmiteNivel(1L);

        BusinessMessages resultado = mensajes(service.validateInsert(ciclo(gradoCF, null)));

        assertEquals(1, resultado.size());
        assertEquals("El nivel es obligatorio para el grado indicado", resultado.get(0).getMessage());
        assertEquals("nivel", resultado.get(0).getFieldName());
    }

    @Test
    void validateInsert_gradoQueAdmiteNivelConNivelDeOtroGrado_devuelveMensajeNivelDeOtroGrado() {
        Grado gradoA = gradoQueAdmiteNivel(1L);
        Grado gradoB = gradoQueAdmiteNivel(2L);
        Nivel nivelDeB = nivel(20L, gradoB);

        BusinessMessages resultado = mensajes(service.validateInsert(ciclo(gradoA, nivelDeB)));

        assertEquals(1, resultado.size());
        assertEquals("El nivel indicado no pertenece al grado del ciclo", resultado.get(0).getMessage());
        assertEquals("nivel", resultado.get(0).getFieldName());
    }

    @Test
    void validateInsert_gradoQueNoAdmiteNivelSinNivel_devuelveOptionalVacio() {
        Grado gradoCE = gradoSinNiveles(3L);

        Optional<BusinessMessages> resultado = service.validateInsert(ciclo(gradoCE, null));

        assertTrue(resultado.isEmpty());
    }

    @Test
    void validateInsert_gradoQueNoAdmiteNivelConNivel_devuelveMensajeGradoSinNivel() {
        Grado gradoCE = gradoSinNiveles(3L);
        Nivel nivelAjeno = nivel(10L, gradoQueAdmiteNivel(1L));

        BusinessMessages resultado = mensajes(service.validateInsert(ciclo(gradoCE, nivelAjeno)));

        assertEquals(1, resultado.size());
        assertEquals("El grado indicado no admite nivel: el nivel debe quedar vacío",
                resultado.get(0).getMessage());
        assertEquals("nivel", resultado.get(0).getFieldName());
    }

    @Test
    void validateInsert_gradoConTodosSusNivelesArchivadosYNivelDeEseGrado_devuelveMensajeGradoSinNivel() {
        Grado gradoArchivado = gradoConTodosSusNivelesArchivados(4L);
        Nivel nivelArchivado = gradoArchivado.getNiveles().get(0);

        BusinessMessages resultado = mensajes(service.validateInsert(ciclo(gradoArchivado, nivelArchivado)));

        assertEquals(1, resultado.size());
        assertEquals("El grado indicado no admite nivel: el nivel debe quedar vacío",
                resultado.get(0).getMessage());
        assertEquals("nivel", resultado.get(0).getFieldName());
    }

    @Test
    void validateInsert_cicloSinGrado_devuelveOptionalVacio() {
        Optional<BusinessMessages> resultado = service.validateInsert(ciclo(null, null));

        assertTrue(resultado.isEmpty());
    }

    @Test
    void validateInsert_cicloSinGradoYConNivel_devuelveOptionalVacio() {
        Nivel nivelCualquiera = nivel(10L, gradoQueAdmiteNivel(1L));

        Optional<BusinessMessages> resultado = service.validateInsert(ciclo(null, nivelCualquiera));

        assertTrue(resultado.isEmpty());
    }

    @Test
    void validateInsert_nivelSinGrado_devuelveMensajeNivelDeOtroGrado() {
        Grado gradoCF = gradoQueAdmiteNivel(1L);
        Nivel nivelHuerfano = nivel(10L, null);

        BusinessMessages resultado = mensajes(service.validateInsert(ciclo(gradoCF, nivelHuerfano)));

        assertEquals(1, resultado.size());
        assertEquals("El nivel indicado no pertenece al grado del ciclo", resultado.get(0).getMessage());
        assertEquals("nivel", resultado.get(0).getFieldName());
    }

    /* ------------------------------------------------------------------ */
    /* validateUpdate                                                     */
    /* ------------------------------------------------------------------ */

    @Test
    void validateUpdate_gradoQueAdmiteNivelConNivelDeEseGrado_devuelveOptionalVacio() {
        Grado gradoCF = gradoQueAdmiteNivel(1L);
        Nivel nivelSuperior = nivel(10L, gradoCF);

        Ciclo ciclo = ciclo(gradoCF, nivelSuperior);
        ciclo.setId(100L);
        Ciclo cicloOriginal = ciclo(gradoCF, nivelSuperior);
        cicloOriginal.setId(100L);
        cicloOriginal.setName("Nombre anterior");

        Optional<BusinessMessages> resultado = service.validateUpdate(ciclo, cicloOriginal);

        assertTrue(resultado.isEmpty());
    }

    @Test
    void validateUpdate_cambioAGradoQueNoAdmiteNivelConNivelHeredado_devuelveMensajeGradoSinNivel() {
        Grado gradoCF = gradoQueAdmiteNivel(1L);
        Grado gradoCE = gradoSinNiveles(3L);
        Nivel nivelSuperior = nivel(10L, gradoCF);

        Ciclo cicloOriginal = ciclo(gradoCF, nivelSuperior);
        Ciclo ciclo = ciclo(gradoCE, nivelSuperior);

        BusinessMessages resultado = mensajes(service.validateUpdate(ciclo, cicloOriginal));

        assertEquals(1, resultado.size());
        assertEquals("El grado indicado no admite nivel: el nivel debe quedar vacío",
                resultado.get(0).getMessage());
        assertEquals("nivel", resultado.get(0).getFieldName());
    }

    @Test
    void validateUpdate_cambioAGradoQueNoAdmiteNivelConNivelVaciado_devuelveOptionalVacio() {
        Grado gradoCF = gradoQueAdmiteNivel(1L);
        Grado gradoCE = gradoSinNiveles(3L);
        Nivel nivelSuperior = nivel(10L, gradoCF);

        Ciclo cicloOriginal = ciclo(gradoCF, nivelSuperior);
        Ciclo ciclo = ciclo(gradoCE, null);

        Optional<BusinessMessages> resultado = service.validateUpdate(ciclo, cicloOriginal);

        assertTrue(resultado.isEmpty());
    }

    @Test
    void validateUpdate_nivelReasignadoAOtroGrado_devuelveMensajeNivelDeOtroGrado() {
        Grado gradoAvanzado = gradoQueAdmiteNivel(1L);
        Grado gradoExperto = gradoQueAdmiteNivel(2L);
        Nivel nivelReasignado = nivel(10L, gradoExperto);

        Ciclo ciclo = ciclo(gradoAvanzado, nivelReasignado);
        Ciclo cicloOriginal = ciclo(gradoAvanzado, nivelReasignado);
        cicloOriginal.setName("Nombre anterior");

        BusinessMessages resultado = mensajes(service.validateUpdate(ciclo, cicloOriginal));

        assertEquals(1, resultado.size());
        assertEquals("El nivel indicado no pertenece al grado del ciclo", resultado.get(0).getMessage());
        assertEquals("nivel", resultado.get(0).getFieldName());
    }

    @Test
    void validateUpdate_gradoQueAdmiteNivelSinNivel_devuelveMensajeNivelObligatorio() {
        Grado gradoCPSinNiveles = gradoSinNiveles(5L);
        Grado gradoCPConNivel = gradoQueAdmiteNivel(5L);

        Ciclo cicloOriginal = ciclo(gradoCPSinNiveles, null);
        Ciclo ciclo = ciclo(gradoCPConNivel, null);

        BusinessMessages resultado = mensajes(service.validateUpdate(ciclo, cicloOriginal));

        assertEquals(1, resultado.size());
        assertEquals("El nivel es obligatorio para el grado indicado", resultado.get(0).getMessage());
        assertEquals("nivel", resultado.get(0).getFieldName());
    }

    @Test
    void validateUpdate_originalConGradoDistintoPeroCicloCoherente_devuelveOptionalVacio() {
        Grado gradoCE = gradoSinNiveles(3L);
        Grado gradoCF = gradoQueAdmiteNivel(1L);
        Nivel nivelSuperior = nivel(10L, gradoCF);

        Ciclo cicloOriginal = ciclo(gradoCE, null);
        Ciclo ciclo = ciclo(gradoCF, nivelSuperior);

        Optional<BusinessMessages> resultado = service.validateUpdate(ciclo, cicloOriginal);

        assertTrue(resultado.isEmpty());
    }

    /* ------------------------------------------------------------------ */
    /* allowPropertiesInsert                                              */
    /* ------------------------------------------------------------------ */

    @Test
    void allowPropertiesInsert_permiteLosSeisCamposEditables() {
        AllowProperties allowProperties = service.allowPropertiesInsert();

        assertTrue(allowProperties.allowProperty("code"));
        assertTrue(allowProperties.allowProperty("name"));
        assertTrue(allowProperties.allowProperty("familiaProfesional"));
        assertTrue(allowProperties.allowProperty("grado"));
        assertTrue(allowProperties.allowProperty("nivel"));
        assertTrue(allowProperties.allowProperty("cursos"));
    }

    @Test
    void allowPropertiesInsert_noPermitePropiedadesFueraDeLaLista() {
        AllowProperties allowProperties = service.allowPropertiesInsert();

        assertFalse(allowProperties.allowProperty("archived"));
        assertFalse(allowProperties.allowProperty("createdBy"));
    }

    @Test
    void allowPropertiesInsert_referenciasConMapaInternoVacio() {
        AllowProperties allowProperties = service.allowPropertiesInsert();

        for (String referencia : List.of("grado", "nivel", "familiaProfesional")) {
            AllowProperties interno = allowProperties.innerAllowProperties(referencia);

            assertFalse(interno.allowProperty("code"), referencia);
            assertFalse(interno.allowProperty("name"), referencia);
        }
    }

    @Test
    void allowPropertiesInsert_cursosPermiteElSubarbolDelPanelMaestroDetalle() {
        AllowProperties cursos = service.allowPropertiesInsert().innerAllowProperties("cursos");

        assertTrue(cursos.allowProperty("code"));
        assertTrue(cursos.allowProperty("name"));
        assertTrue(cursos.allowProperty("ciclo"));
        assertTrue(cursos.allowProperty("leyEducativa"));
        assertTrue(cursos.allowProperty("modulos"));
        assertFalse(cursos.allowProperty("archived"));

        AllowProperties modulos = cursos.innerAllowProperties("modulos");
        assertTrue(modulos.allowProperty("curso"));
        assertTrue(modulos.allowProperty("modulo"));
        assertFalse(modulos.allowProperty("archived"));

        AllowProperties leyEducativa = cursos.innerAllowProperties("leyEducativa");
        assertFalse(leyEducativa.allowProperty("code"));
        assertFalse(leyEducativa.allowProperty("name"));
    }

    /* ------------------------------------------------------------------ */
    /* allowPropertiesUpdate                                              */
    /* ------------------------------------------------------------------ */

    @Test
    void allowPropertiesUpdate_permiteLaMismaListaQueElAlta() {
        AllowProperties allowProperties = service.allowPropertiesUpdate();

        assertTrue(allowProperties.allowProperty("code"));
        assertTrue(allowProperties.allowProperty("name"));
        assertTrue(allowProperties.allowProperty("familiaProfesional"));
        assertTrue(allowProperties.allowProperty("grado"));
        assertTrue(allowProperties.allowProperty("nivel"));
        assertTrue(allowProperties.allowProperty("cursos"));
        assertFalse(allowProperties.allowProperty("archived"));
        assertFalse(allowProperties.allowProperty("createdBy"));

        AllowProperties cursos = allowProperties.innerAllowProperties("cursos");
        assertTrue(cursos.allowProperty("code"));
        assertTrue(cursos.allowProperty("name"));
        assertTrue(cursos.allowProperty("ciclo"));
        assertTrue(cursos.allowProperty("leyEducativa"));
        assertTrue(cursos.allowProperty("modulos"));
    }

}
