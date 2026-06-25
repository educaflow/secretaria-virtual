package com.educaflow.system.gruposnotas.service.impl;

import com.axelor.db.Query;
import com.axelor.db.modelservice.AllowProperties;
import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.i18n.I18n;
import com.educaflow.system.gruposnotas.db.AlumnoGrupo;
import com.educaflow.system.gruposnotas.db.EstadoGrupo;
import com.educaflow.system.gruposnotas.db.Grupo;
import com.educaflow.system.gruposnotas.db.ModuloGrupo;
import com.educaflow.system.gruposnotas.db.Nota;
import com.educaflow.system.gruposnotas.db.ValorNota;
import com.educaflow.system.gruposnotas.db.repo.NotaRepository;
import com.educaflow.system.gruposnotas.service.NotaInsertDTO;
import jakarta.validation.ValidationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotaServiceImplTest {

    private NotaRepository repository;
    private NotaServiceImpl service;

    private MockedStatic<I18n> i18nMock;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(NotaRepository.class);
        service = new NotaServiceImpl(Nota.class, repository);

        // lenient: I18n se programa en el setup pero no todos los tests recorren las ramas que lo
        // consumen (happy paths que no producen mensaje).
        i18nMock = Mockito.mockStatic(I18n.class,
                Mockito.withSettings().strictness(org.mockito.quality.Strictness.LENIENT));
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

    private String mensaje(Optional<BusinessMessages> optional) {
        assertTrue(optional.isPresent());
        return optional.get().get(0).getMessage();
    }

    private Grupo grupo(EstadoGrupo estado) {
        Grupo grupo = new Grupo();
        grupo.setEstado(estado);
        return grupo;
    }

    private ModuloGrupo moduloGrupo(EstadoGrupo estado) {
        ModuloGrupo moduloGrupo = new ModuloGrupo();
        moduloGrupo.setGrupo(grupo(estado));
        return moduloGrupo;
    }

    private Nota nota(ModuloGrupo moduloGrupo, ValorNota valor) {
        Nota nota = new Nota();
        nota.setModuloGrupo(moduloGrupo);
        nota.setValor(valor);
        return nota;
    }

    @SuppressWarnings("unchecked")
    private void programarContadorMatriculasHonor(long count) {
        Query<Nota> query = Mockito.mock(Query.class);
        when(repository.countMatriculasHonorByModuloGrupo(any(), anyLong())).thenReturn(query);
        when(query.count()).thenReturn(count);
    }

    /* ------------------------------------------------------------------ */
    /* validateUpdate                                                     */
    /* ------------------------------------------------------------------ */

    @Test
    void validateUpdate_grupoCerrado_devuelveError() {
        Nota nota = nota(moduloGrupo(EstadoGrupo.CERRADO), ValorNota.NOTA_8);
        Nota original = nota(moduloGrupo(EstadoGrupo.CERRADO), ValorNota.NO_EVALUADO);

        Optional<BusinessMessages> resultado = service.validateUpdate(nota, original);

        assertEquals("No se pueden modificar las notas de un grupo cerrado", mensaje(resultado));
    }

    @Test
    void validateUpdate_valorFueraDelDominio_devuelveError() {
        Nota nota = nota(moduloGrupo(EstadoGrupo.ABIERTO), null);
        Nota original = nota(moduloGrupo(EstadoGrupo.ABIERTO), ValorNota.NO_EVALUADO);

        Optional<BusinessMessages> resultado = service.validateUpdate(nota, original);

        assertEquals("La nota debe ser No evaluado, un número entero del 1 al 10 o Matrícula de Honor",
                mensaje(resultado));
    }

    @Test
    void validateUpdate_cuartaMatriculaHonorEnModulo_devuelveError() {
        Nota nota = nota(moduloGrupo(EstadoGrupo.ABIERTO), ValorNota.MATRICULA_HONOR);
        Nota original = nota(moduloGrupo(EstadoGrupo.ABIERTO), ValorNota.NO_EVALUADO);
        programarContadorMatriculasHonor(3L);

        Optional<BusinessMessages> resultado = service.validateUpdate(nota, original);

        assertEquals("No se pueden poner más de 3 matrículas de honor en un módulo", mensaje(resultado));
    }

    @Test
    void validateUpdate_terceraMatriculaHonorEnModulo_devuelveVacio() {
        Nota nota = nota(moduloGrupo(EstadoGrupo.ABIERTO), ValorNota.MATRICULA_HONOR);
        Nota original = nota(moduloGrupo(EstadoGrupo.ABIERTO), ValorNota.NO_EVALUADO);
        programarContadorMatriculasHonor(2L);

        Optional<BusinessMessages> resultado = service.validateUpdate(nota, original);

        assertTrue(resultado.isEmpty());
    }

    @Test
    void validateUpdate_valorNumericoValidoGrupoAbierto_devuelveVacio() {
        Nota nota = nota(moduloGrupo(EstadoGrupo.ABIERTO), ValorNota.NOTA_8);
        Nota original = nota(moduloGrupo(EstadoGrupo.ABIERTO), ValorNota.NO_EVALUADO);

        Optional<BusinessMessages> resultado = service.validateUpdate(nota, original);

        assertTrue(resultado.isEmpty());
    }

    /* ------------------------------------------------------------------ */
    /* validateInsert                                                     */
    /* ------------------------------------------------------------------ */

    @Test
    void validateInsert_dtoCompleto_devuelveVacio() {
        NotaInsertDTO dto = new NotaInsertDTO(new ModuloGrupo(), new AlumnoGrupo());

        Optional<BusinessMessages> resultado = service.validateInsert(dto);

        assertTrue(resultado.isEmpty());
    }

    @Test
    void validateInsert_moduloGrupoNulo_devuelveError() {
        NotaInsertDTO dto = new NotaInsertDTO(null, new AlumnoGrupo());

        Optional<BusinessMessages> resultado = service.validateInsert(dto);

        assertTrue(resultado.isPresent());
    }

    /* ------------------------------------------------------------------ */
    /* insert                                                             */
    /* ------------------------------------------------------------------ */

    @Test
    void insert_creaNotaNoEvaluadoSinFechas() {
        ModuloGrupo moduloGrupo = new ModuloGrupo();
        AlumnoGrupo alumnoGrupo = new AlumnoGrupo();
        NotaInsertDTO dto = new NotaInsertDTO(moduloGrupo, alumnoGrupo);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Nota resultado = service.insert(dto);

        assertEquals(ValorNota.NO_EVALUADO, resultado.getValor());
        assertNull(resultado.getFechaCalificacion());
        assertNull(resultado.getFechaUltimaModificacion());
        verify(repository).save(any());
    }

    /* ------------------------------------------------------------------ */
    /* update                                                             */
    /* ------------------------------------------------------------------ */

    @Test
    void update_primeraCalificacion_asignaFechaCalificacionYNoUltimaModificacion() {
        ModuloGrupo moduloGrupo = moduloGrupo(EstadoGrupo.ABIERTO);
        Nota original = nota(moduloGrupo, ValorNota.NO_EVALUADO);
        original.setFechaCalificacion(null);
        Nota nota = nota(moduloGrupo, ValorNota.NOTA_8);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.update(nota, original);

        assertNotNull(nota.getFechaCalificacion());
        assertNull(nota.getFechaUltimaModificacion());
        verify(repository).save(nota);
    }

    @Test
    void update_recalificaNotaYaCalificada_asignaFechaUltimaModificacion() {
        ModuloGrupo moduloGrupo = moduloGrupo(EstadoGrupo.ABIERTO);
        LocalDateTime fechaPrevia = LocalDateTime.of(2024, 1, 1, 10, 0);
        Nota original = nota(moduloGrupo, ValorNota.NOTA_8);
        original.setFechaCalificacion(fechaPrevia);
        Nota nota = nota(moduloGrupo, ValorNota.NOTA_6);
        nota.setFechaCalificacion(fechaPrevia);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.update(nota, original);

        assertNotNull(nota.getFechaUltimaModificacion());
        assertEquals(fechaPrevia, nota.getFechaCalificacion());
        verify(repository).save(nota);
    }

    @Test
    void update_restauraModuloGrupoYAlumnoGrupoDesdeOriginal() {
        ModuloGrupo moduloGrupoOriginal = moduloGrupo(EstadoGrupo.ABIERTO);
        AlumnoGrupo alumnoGrupoOriginal = new AlumnoGrupo();
        Nota original = nota(moduloGrupoOriginal, ValorNota.NO_EVALUADO);
        original.setAlumnoGrupo(alumnoGrupoOriginal);

        ModuloGrupo moduloGrupoManipulado = moduloGrupo(EstadoGrupo.ABIERTO);
        AlumnoGrupo alumnoGrupoManipulado = new AlumnoGrupo();
        Nota nota = nota(moduloGrupoManipulado, ValorNota.NOTA_7);
        nota.setAlumnoGrupo(alumnoGrupoManipulado);

        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.update(nota, original);

        assertSame(moduloGrupoOriginal, nota.getModuloGrupo());
        assertSame(alumnoGrupoOriginal, nota.getAlumnoGrupo());
    }

    @Test
    void update_grupoCerrado_lanzaExcepcionYNoPersiste() {
        Nota nota = nota(moduloGrupo(EstadoGrupo.CERRADO), ValorNota.NOTA_8);
        Nota original = nota(moduloGrupo(EstadoGrupo.CERRADO), ValorNota.NO_EVALUADO);

        ValidationException ex = assertThrows(ValidationException.class,
                () -> service.update(nota, original));
        assertEquals("No se pueden modificar las notas de un grupo cerrado", ex.getMessage());
        verify(repository, never()).save(any());
    }

    /* ------------------------------------------------------------------ */
    /* allowProperties                                                    */
    /* ------------------------------------------------------------------ */

    @Test
    void allowPropertiesUpdate_incluyeSoloValor() {
        AllowProperties allowProperties = service.allowPropertiesUpdate();

        assertTrue(allowProperties.allowProperty("valor"));
        assertFalse(allowProperties.allowProperty("moduloGrupo"));
        assertFalse(allowProperties.allowProperty("alumnoGrupo"));
        assertFalse(allowProperties.allowProperty("fechaCalificacion"));
        assertFalse(allowProperties.allowProperty("fechaUltimaModificacion"));
    }
}
