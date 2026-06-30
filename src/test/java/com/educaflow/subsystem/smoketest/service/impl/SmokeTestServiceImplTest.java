package com.educaflow.subsystem.smoketest.service.impl;

import com.axelor.db.modelservice.AllowProperties;
import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.i18n.I18n;
import com.educaflow.subsystem.smoketest.db.SmokeTest;
import com.educaflow.subsystem.smoketest.db.repo.SmokeTestRepository;
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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SmokeTestServiceImplTest {

    private SmokeTestRepository repository;
    private SmokeTestServiceImpl service;

    private MockedStatic<I18n> i18nMock;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(SmokeTestRepository.class);
        service = new SmokeTestServiceImpl(SmokeTest.class, repository);

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

    private SmokeTest smokeTest(String texto) {
        SmokeTest smokeTest = new SmokeTest();
        smokeTest.setTexto(texto);
        return smokeTest;
    }

    /* ------------------------------------------------------------------ */
    /* validateInsert                                                     */
    /* ------------------------------------------------------------------ */

    @Test
    void validateInsert_textoValido_devuelveVacio() {
        SmokeTest smokeTest = smokeTest("Prueba de humo 1");

        Optional<BusinessMessages> resultado = service.validateInsert(smokeTest);

        assertTrue(resultado.isEmpty());
    }

    @Test
    void validateInsert_textoNulo_devuelveError() {
        SmokeTest smokeTest = smokeTest(null);

        Optional<BusinessMessages> resultado = service.validateInsert(smokeTest);

        assertEquals("El texto es obligatorio", mensaje(resultado));
    }

    @Test
    void validateInsert_textoVacio_devuelveError() {
        SmokeTest smokeTest = smokeTest("");

        Optional<BusinessMessages> resultado = service.validateInsert(smokeTest);

        assertEquals("El texto es obligatorio", mensaje(resultado));
    }

    @Test
    void validateInsert_textoSoloEspacios_devuelveError() {
        SmokeTest smokeTest = smokeTest("   ");

        Optional<BusinessMessages> resultado = service.validateInsert(smokeTest);

        assertEquals("El texto es obligatorio", mensaje(resultado));
    }

    /* ------------------------------------------------------------------ */
    /* validateUpdate                                                     */
    /* ------------------------------------------------------------------ */

    @Test
    void validateUpdate_textoValido_devuelveVacio() {
        SmokeTest smokeTest = smokeTest("Prueba de humo 3 editada");
        SmokeTest original = smokeTest("Prueba de humo 3");

        Optional<BusinessMessages> resultado = service.validateUpdate(smokeTest, original);

        assertTrue(resultado.isEmpty());
    }

    @Test
    void validateUpdate_textoNulo_devuelveError() {
        SmokeTest smokeTest = smokeTest(null);
        SmokeTest original = smokeTest("Prueba de humo 3");

        Optional<BusinessMessages> resultado = service.validateUpdate(smokeTest, original);

        assertEquals("El texto es obligatorio", mensaje(resultado));
    }

    @Test
    void validateUpdate_textoSoloEspacios_devuelveError() {
        SmokeTest smokeTest = smokeTest("  ");
        SmokeTest original = smokeTest("Prueba de humo 3");

        Optional<BusinessMessages> resultado = service.validateUpdate(smokeTest, original);

        assertEquals("El texto es obligatorio", mensaje(resultado));
    }

    /* ------------------------------------------------------------------ */
    /* insert                                                             */
    /* ------------------------------------------------------------------ */

    @Test
    void insert_textoValido_sellaAmbasFechasYPersiste() {
        SmokeTest smokeTest = smokeTest("Prueba de humo 1");
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        LocalDateTime antes = LocalDateTime.now();
        service.insert(smokeTest);

        assertNotNull(smokeTest.getFechaCreacion());
        assertNotNull(smokeTest.getFechaUltimaModificacion());
        assertTrue(!smokeTest.getFechaCreacion().isBefore(antes));
        assertTrue(!smokeTest.getFechaUltimaModificacion().isBefore(antes));
        verify(repository).save(smokeTest);
    }

    @Test
    void insert_clienteIntentaDictarFechas_servidorLasSobrescribe() {
        LocalDateTime falsa = LocalDateTime.of(2000, 1, 1, 0, 0);
        SmokeTest smokeTest = smokeTest("x");
        smokeTest.setFechaCreacion(falsa);
        smokeTest.setFechaUltimaModificacion(falsa);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        LocalDateTime antes = LocalDateTime.now();
        service.insert(smokeTest);

        assertNotEquals(falsa, smokeTest.getFechaCreacion());
        assertNotEquals(falsa, smokeTest.getFechaUltimaModificacion());
        assertTrue(!smokeTest.getFechaCreacion().isBefore(antes));
        assertTrue(!smokeTest.getFechaUltimaModificacion().isBefore(antes));
        verify(repository).save(smokeTest);
    }

    @Test
    void insert_textoNulo_lanzaValidationExceptionYNoPersiste() {
        SmokeTest smokeTest = smokeTest(null);

        ValidationException ex = assertThrows(ValidationException.class,
                () -> service.insert(smokeTest));
        assertEquals("El texto es obligatorio", ex.getMessage());
        verify(repository, never()).save(any());
    }

    /* ------------------------------------------------------------------ */
    /* update                                                             */
    /* ------------------------------------------------------------------ */

    @Test
    void update_textoValido_refrescaUltimaModificacionYMantieneCreacion() {
        LocalDateTime fechaPrevia = LocalDateTime.of(2024, 1, 1, 10, 0);
        SmokeTest original = smokeTest("Prueba de humo 3");
        original.setFechaCreacion(fechaPrevia);
        original.setFechaUltimaModificacion(fechaPrevia);
        SmokeTest smokeTest = smokeTest("Prueba de humo 3 editada");
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        LocalDateTime antes = LocalDateTime.now();
        service.update(smokeTest, original);

        assertEquals(fechaPrevia, smokeTest.getFechaCreacion());
        assertNotNull(smokeTest.getFechaUltimaModificacion());
        assertTrue(!smokeTest.getFechaUltimaModificacion().isBefore(antes));
        verify(repository).save(smokeTest);
    }

    @Test
    void update_clienteManipulaFechaCreacion_seRestauraDesdeOriginal() {
        LocalDateTime fechaOriginal = LocalDateTime.of(2024, 1, 1, 10, 0);
        SmokeTest original = smokeTest("Prueba de humo 3");
        original.setFechaCreacion(fechaOriginal);
        SmokeTest smokeTest = smokeTest("x");
        smokeTest.setFechaCreacion(LocalDateTime.of(2099, 12, 31, 23, 59));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.update(smokeTest, original);

        assertEquals(fechaOriginal, smokeTest.getFechaCreacion());
        verify(repository).save(smokeTest);
    }

    @Test
    void update_textoNulo_lanzaValidationExceptionYNoPersiste() {
        SmokeTest smokeTest = smokeTest(null);
        SmokeTest original = smokeTest("Prueba de humo 3");

        ValidationException ex = assertThrows(ValidationException.class,
                () -> service.update(smokeTest, original));
        assertEquals("El texto es obligatorio", ex.getMessage());
        verify(repository, never()).save(any());
    }

    /* ------------------------------------------------------------------ */
    /* allowProperties                                                    */
    /* ------------------------------------------------------------------ */

    @Test
    void allowPropertiesInsert_incluyeSoloTexto() {
        AllowProperties allowProperties = service.allowPropertiesInsert();

        assertTrue(allowProperties.allowProperty("texto"));
        assertFalse(allowProperties.allowProperty("fechaCreacion"));
        assertFalse(allowProperties.allowProperty("fechaUltimaModificacion"));
    }

    @Test
    void allowPropertiesUpdate_incluyeSoloTexto() {
        AllowProperties allowProperties = service.allowPropertiesUpdate();

        assertTrue(allowProperties.allowProperty("texto"));
        assertFalse(allowProperties.allowProperty("fechaCreacion"));
        assertFalse(allowProperties.allowProperty("fechaUltimaModificacion"));
    }
}
