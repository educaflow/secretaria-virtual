package com.educaflow.system.gruposnotas.service.impl;

import com.axelor.db.Repository;
import com.axelor.db.modelservice.AllowProperties;
import com.axelor.db.modelservice.BusinessMessages;
import com.educaflow.system.gruposnotas.db.AlumnoGrupo;
import com.educaflow.system.gruposnotas.db.EstadoGrupo;
import com.educaflow.system.gruposnotas.db.Grupo;
import com.educaflow.system.gruposnotas.db.ModuloGrupo;
import com.educaflow.system.gruposnotas.db.Nota;
import com.educaflow.system.gruposnotas.db.ValorNota;
import com.educaflow.system.gruposnotas.db.repo.NotaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios de {@link NotaServiceImpl}.
 *
 * <p>El servicio extiende {@code DefaultModelService<Nota>} y se construye con
 * {@code (Class<Nota>, Repository<Nota>)}. El colaborador {@code @Inject notaRepository}
 * se inyecta por reflexión.</p>
 *
 * <p>Notas sobre el código real frente a la descripción del contrato:</p>
 * <ul>
 *   <li>{@code guardarNota} persiste con {@code repository.save(nota)} (el
 *       {@code Repository<Nota>} heredado pasado al constructor), no con
 *       {@code notaRepository.save(...)}; por eso los {@code verify} de guardado/no-guardado
 *       se hacen sobre el {@code repository}.</li>
 *   <li>{@code validateInsert} comprueba que {@code moduloGrupo} y {@code alumnoGrupo}
 *       no sean nulos y respalda la unicidad (alumno+módulo) con una consulta mockeable del
 *       repositorio ({@code notaRepository.countByModuloGrupoYAlumnoGrupo(...)}), defensa en
 *       profundidad de la unique-constraint de la BD (V-Nota-005 / RES-006). Ver los tests
 *       {@code validateInsert_notaUnica_devuelveOptionalVacio} y
 *       {@code validateInsert_notaDuplicadaParaAlumnoYModulo_devuelveMensaje}.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class NotaServiceImplTest {

    @Mock
    private Repository<Nota> repository;

    @Mock
    private NotaRepository notaRepository;

    private NotaServiceImpl service;

    @BeforeEach
    void setUp() throws Exception {
        service = new NotaServiceImpl(Nota.class, repository);
        inject(service, "notaRepository", notaRepository);
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    private static void inject(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static Grupo grupo(EstadoGrupo estado) {
        Grupo grupo = new Grupo();
        grupo.setEstado(estado);
        return grupo;
    }

    private static ModuloGrupo moduloGrupoConGrupo(EstadoGrupo estado) {
        ModuloGrupo moduloGrupo = new ModuloGrupo();
        moduloGrupo.setGrupo(grupo(estado));
        return moduloGrupo;
    }

    private static Nota nota(ValorNota valor, ModuloGrupo moduloGrupo) {
        Nota nota = new Nota();
        nota.setValor(valor);
        nota.setModuloGrupo(moduloGrupo);
        return nota;
    }

    private static Nota notaOriginal(ValorNota valor) {
        Nota nota = new Nota();
        nota.setValor(valor);
        return nota;
    }

    /** Texto del único {@link com.axelor.db.modelservice.BusinessMessage} presente. */
    private static String mensaje(Optional<BusinessMessages> resultado) {
        assertTrue(resultado.isPresent(), "Se esperaba un BusinessMessages presente");
        assertEquals(1, resultado.get().size(), "Se esperaba exactamente un mensaje");
        return resultado.get().get(0).getMessage();
    }

    /** Recorre la cadena de causas buscando el texto esperado en algún mensaje. */
    private static void assertCadenaContieneMensaje(Throwable ex, String mensajeEsperado) {
        Throwable cause = ex;
        while (cause != null) {
            if (cause.getMessage() != null && cause.getMessage().contains(mensajeEsperado)) {
                return;
            }
            cause = cause.getCause();
        }
        throw new AssertionError("No se encontró el mensaje '" + mensajeEsperado
                + "' en la cadena de causas de " + ex);
    }

    // =====================================================================
    // validateInsert
    // =====================================================================

    @Test
    void validateInsert_notaUnica_devuelveOptionalVacio() {
        ModuloGrupo moduloGrupo = new ModuloGrupo();
        AlumnoGrupo alumnoGrupo = new AlumnoGrupo();
        Nota nota = new Nota();
        nota.setModuloGrupo(moduloGrupo);
        nota.setAlumnoGrupo(alumnoGrupo);

        // Unicidad: no existe otra nota para ese moduloGrupo+alumnoGrupo.
        when(notaRepository.countByModuloGrupoYAlumnoGrupo(moduloGrupo, alumnoGrupo)).thenReturn(0L);

        Optional<BusinessMessages> resultado = service.validateInsert(nota);

        assertTrue(resultado.isEmpty());
    }

    @Test
    void validateInsert_notaDuplicadaParaAlumnoYModulo_devuelveMensaje() {
        // V-Nota-005 (RES-006): ya existe una nota para ese moduloGrupo+alumnoGrupo.
        ModuloGrupo moduloGrupo = new ModuloGrupo();
        AlumnoGrupo alumnoGrupo = new AlumnoGrupo();
        Nota nota = new Nota();
        nota.setModuloGrupo(moduloGrupo);
        nota.setAlumnoGrupo(alumnoGrupo);

        when(notaRepository.countByModuloGrupoYAlumnoGrupo(moduloGrupo, alumnoGrupo)).thenReturn(1L);

        Optional<BusinessMessages> resultado = service.validateInsert(nota);

        assertEquals("Ya existe una nota para ese alumno y módulo.", mensaje(resultado));
    }

    // =====================================================================
    // validateGuardarNota
    // =====================================================================

    @Test
    void validateGuardarNota_valorValidoGrupoAbierto_devuelveOptionalVacio() {
        Nota nota = nota(ValorNota.NOTA_08, moduloGrupoConGrupo(EstadoGrupo.ABIERTO));
        Nota original = notaOriginal(ValorNota.NO_EVALUADO);

        Optional<BusinessMessages> resultado = service.validateGuardarNota(nota, original);

        assertTrue(resultado.isEmpty());
    }

    @Test
    void validateGuardarNota_grupoCerrado_devuelveMensajeNoModificarNotas() {
        Nota nota = nota(ValorNota.NOTA_08, moduloGrupoConGrupo(EstadoGrupo.CERRADO));
        Nota original = notaOriginal(ValorNota.NO_EVALUADO);

        Optional<BusinessMessages> resultado = service.validateGuardarNota(nota, original);

        assertEquals("No se pueden modificar las notas de un grupo cerrado.", mensaje(resultado));
    }

    @Test
    void validateGuardarNota_cuartaMatriculaHonor_devuelveMensajeMax3() {
        ModuloGrupo moduloGrupo = moduloGrupoConGrupo(EstadoGrupo.ABIERTO);
        Nota nota = nota(ValorNota.MATRICULA_HONOR, moduloGrupo);
        Nota original = notaOriginal(ValorNota.NOTA_08);

        when(notaRepository.countMatriculasHonorByModuloGrupo(moduloGrupo)).thenReturn(3L);

        Optional<BusinessMessages> resultado = service.validateGuardarNota(nota, original);

        assertEquals("No se pueden poner más de 3 matrículas de honor en un módulo.",
                mensaje(resultado));
    }

    @Test
    void validateGuardarNota_terceraMatriculaHonor_devuelveOptionalVacio() {
        ModuloGrupo moduloGrupo = moduloGrupoConGrupo(EstadoGrupo.ABIERTO);
        Nota nota = nota(ValorNota.MATRICULA_HONOR, moduloGrupo);
        Nota original = notaOriginal(ValorNota.NOTA_08);

        // Esta sería la 3ª MH (count = 2 antes de aplicarla): permitida.
        when(notaRepository.countMatriculasHonorByModuloGrupo(moduloGrupo)).thenReturn(2L);

        Optional<BusinessMessages> resultado = service.validateGuardarNota(nota, original);

        assertTrue(resultado.isEmpty());
        verify(notaRepository).countMatriculasHonorByModuloGrupo(moduloGrupo);
    }

    @Test
    void validateGuardarNota_yaEraMatriculaHonor_noCuentaContraLimite() {
        ModuloGrupo moduloGrupo = moduloGrupoConGrupo(EstadoGrupo.ABIERTO);
        Nota nota = nota(ValorNota.MATRICULA_HONOR, moduloGrupo);
        // Ya era MH: no se pasa de no-MH a MH, así que el límite no aplica.
        Nota original = notaOriginal(ValorNota.MATRICULA_HONOR);

        Optional<BusinessMessages> resultado = service.validateGuardarNota(nota, original);

        assertTrue(resultado.isEmpty());
        verify(notaRepository, never()).countMatriculasHonorByModuloGrupo(any());
    }

    @Test
    void validateGuardarNota_valorFueraDeDominio_devuelveMensajeNotaInvalida() {
        // valor = null es el único caso testeable a nivel de servicio (valor es enum).
        Nota nota = nota(null, moduloGrupoConGrupo(EstadoGrupo.ABIERTO));
        Nota original = notaOriginal(ValorNota.NO_EVALUADO);

        Optional<BusinessMessages> resultado = service.validateGuardarNota(nota, original);

        assertEquals(
                "La nota debe ser No evaluado, un número entero del 1 al 10 o Matrícula de Honor.",
                mensaje(resultado));
    }

    // =====================================================================
    // guardarNota
    // =====================================================================

    @Test
    void guardarNota_primeraCalificacion_fijaFechaCalificacion() {
        Nota nota = nota(ValorNota.NOTA_08, moduloGrupoConGrupo(EstadoGrupo.ABIERTO));
        Nota original = notaOriginal(ValorNota.NO_EVALUADO);
        original.setFechaCalificacion(null);
        original.setFechaUltimaModificacion(null);

        when(repository.save(nota)).thenReturn(nota);

        service.guardarNota(nota, original);

        assertNotNull(nota.getFechaCalificacion());
        assertNull(nota.getFechaUltimaModificacion());
        verify(repository).save(nota);
    }

    @Test
    void guardarNota_modificacionPosterior_fijaFechaUltimaModificacion() {
        Nota nota = nota(ValorNota.NOTA_06, moduloGrupoConGrupo(EstadoGrupo.ABIERTO));
        Nota original = notaOriginal(ValorNota.NOTA_08);
        LocalDateTime fechaCalificacionPrevia = LocalDateTime.of(2024, 1, 10, 9, 0);
        original.setFechaCalificacion(fechaCalificacionPrevia);

        when(repository.save(nota)).thenReturn(nota);

        service.guardarNota(nota, original);

        assertNotNull(nota.getFechaUltimaModificacion());
        assertEquals(fechaCalificacionPrevia, nota.getFechaCalificacion());
    }

    @Test
    void guardarNota_noCambiaValor_noTocaFechas() {
        Nota nota = nota(ValorNota.NOTA_08, moduloGrupoConGrupo(EstadoGrupo.ABIERTO));
        Nota original = notaOriginal(ValorNota.NOTA_08);
        LocalDateTime fechaCalificacionPrevia = LocalDateTime.of(2024, 1, 10, 9, 0);
        original.setFechaCalificacion(fechaCalificacionPrevia);
        original.setFechaUltimaModificacion(null);

        when(repository.save(nota)).thenReturn(nota);

        service.guardarNota(nota, original);

        assertEquals(fechaCalificacionPrevia, nota.getFechaCalificacion());
        assertNull(nota.getFechaUltimaModificacion());
    }

    @Test
    void guardarNota_clienteIntentaDictarFechas_seRestauranDesdeOriginal() {
        Nota nota = nota(ValorNota.NOTA_08, moduloGrupoConGrupo(EstadoGrupo.ABIERTO));
        // El cliente intenta dictar fechas manipuladas.
        nota.setFechaCalificacion(LocalDateTime.of(2000, 1, 1, 0, 0));
        nota.setFechaUltimaModificacion(LocalDateTime.of(2000, 1, 1, 0, 0));

        Nota original = notaOriginal(ValorNota.NOTA_08);
        LocalDateTime fechaCalificacionReal = LocalDateTime.of(2024, 1, 10, 9, 0);
        original.setFechaCalificacion(fechaCalificacionReal);
        original.setFechaUltimaModificacion(null);

        when(repository.save(nota)).thenReturn(nota);

        service.guardarNota(nota, original);

        // Valor sin cambiar: ninguna rama toca las fechas; quedan las del original, no las del cliente.
        assertEquals(fechaCalificacionReal, nota.getFechaCalificacion());
        assertNull(nota.getFechaUltimaModificacion());
    }

    @Test
    void guardarNota_grupoCerrado_lanzaExcepcionYNoGuarda() {
        Nota nota = nota(ValorNota.NOTA_08, moduloGrupoConGrupo(EstadoGrupo.CERRADO));
        Nota original = notaOriginal(ValorNota.NO_EVALUADO);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.guardarNota(nota, original));

        assertCadenaContieneMensaje(ex, "No se pueden modificar las notas de un grupo cerrado.");
        verify(repository, never()).save(any());
    }

    @Test
    void guardarNota_cuartaMatriculaHonor_lanzaExcepcionYNoGuarda() {
        ModuloGrupo moduloGrupo = moduloGrupoConGrupo(EstadoGrupo.ABIERTO);
        Nota nota = nota(ValorNota.MATRICULA_HONOR, moduloGrupo);
        Nota original = notaOriginal(ValorNota.NOTA_08);

        when(notaRepository.countMatriculasHonorByModuloGrupo(moduloGrupo)).thenReturn(3L);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.guardarNota(nota, original));

        assertCadenaContieneMensaje(ex,
                "No se pueden poner más de 3 matrículas de honor en un módulo.");
        verify(repository, never()).save(any());
    }

    // =====================================================================
    // allowProperties*
    // =====================================================================

    @Test
    void allowPropertiesGuardarNota_soloValor() {
        AllowProperties allow = service.allowPropertiesGuardarNota();

        assertTrue(allow.allowProperty("valor"));
        assertFalse(allow.allowProperty("moduloGrupo"));
        assertFalse(allow.allowProperty("alumnoGrupo"));
        assertFalse(allow.allowProperty("fechaCalificacion"));
        assertFalse(allow.allowProperty("fechaUltimaModificacion"));
    }

    @Test
    void allowPropertiesInsert_denyAll() {
        AllowProperties allow = service.allowPropertiesInsert();

        assertFalse(allow.allowProperty("valor"));
        assertFalse(allow.allowProperty("moduloGrupo"));
        assertFalse(allow.allowProperty("alumnoGrupo"));
    }

    @Test
    void allowPropertiesUpdate_denyAll() {
        AllowProperties allow = service.allowPropertiesUpdate();

        assertFalse(allow.allowProperty("valor"));
        assertFalse(allow.allowProperty("moduloGrupo"));
        assertFalse(allow.allowProperty("alumnoGrupo"));
    }

    @Test
    void allowPropertiesRemove_denyAll() {
        AllowProperties allow = service.allowPropertiesRemove();

        assertFalse(allow.allowProperty("valor"));
        assertFalse(allow.allowProperty("moduloGrupo"));
        assertFalse(allow.allowProperty("alumnoGrupo"));
    }
}
