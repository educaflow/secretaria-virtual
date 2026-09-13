package com.educaflow.subsystem.sistemaeducativo.service.impl;

import com.axelor.db.modelservice.AllowProperties;
import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.i18n.I18n;
import com.educaflow.subsystem.sistemaeducativo.db.Grado;
import com.educaflow.subsystem.sistemaeducativo.db.Nivel;
import com.educaflow.subsystem.sistemaeducativo.db.repo.NivelRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class NivelServiceImplTest {

    private NivelRepository repository;
    private NivelServiceImpl service;

    private MockedStatic<I18n> i18nMock;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(NivelRepository.class);
        service = new NivelServiceImpl(Nivel.class, repository);

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

    private Grado grado(long id) {
        Grado grado = new Grado();
        grado.setId(id);
        return grado;
    }

    private Nivel nivel(Grado grado) {
        Nivel nivel = new Nivel();
        nivel.setCode("BAS");
        nivel.setName("Básico");
        nivel.setGrado(grado);
        return nivel;
    }

    /* ------------------------------------------------------------------ */
    /* validateInsert                                                     */
    /* ------------------------------------------------------------------ */

    @Test
    void validateInsert_nivelConGrado_devuelveOptionalVacio() {
        Nivel nivel = nivel(grado(1L));

        Optional<BusinessMessages> resultado = service.validateInsert(nivel);

        assertTrue(resultado.isEmpty());
    }

    @Test
    void validateInsert_nivelSinGrado_devuelveMensajeGradoObligatorio() {
        Nivel nivel = nivel(null);

        BusinessMessages resultado = mensajes(service.validateInsert(nivel));

        assertEquals(1, resultado.size());
        assertEquals("El grado es obligatorio", resultado.get(0).getMessage());
        assertEquals("grado", resultado.get(0).getFieldName());
    }

    /* ------------------------------------------------------------------ */
    /* validateUpdate                                                     */
    /* ------------------------------------------------------------------ */

    @Test
    void validateUpdate_nivelConGrado_devuelveOptionalVacio() {
        Nivel nivel = nivel(grado(1L));
        Nivel nivelOriginal = nivel(grado(1L));
        nivelOriginal.setName("Nombre anterior");

        Optional<BusinessMessages> resultado = service.validateUpdate(nivel, nivelOriginal);

        assertTrue(resultado.isEmpty());
    }

    @Test
    void validateUpdate_nivelSinGrado_devuelveMensajeGradoObligatorio() {
        Nivel nivel = nivel(null);
        Nivel nivelOriginal = nivel(grado(1L));

        BusinessMessages resultado = mensajes(service.validateUpdate(nivel, nivelOriginal));

        assertEquals(1, resultado.size());
        assertEquals("El grado es obligatorio", resultado.get(0).getMessage());
        assertEquals("grado", resultado.get(0).getFieldName());
    }

    @Test
    void validateUpdate_cambioDeGradoAOtroGradoValido_devuelveOptionalVacio() {
        Nivel nivel = nivel(grado(2L));
        Nivel nivelOriginal = nivel(grado(1L));

        Optional<BusinessMessages> resultado = service.validateUpdate(nivel, nivelOriginal);

        assertTrue(resultado.isEmpty());
    }

    /* ------------------------------------------------------------------ */
    /* allowPropertiesInsert                                              */
    /* ------------------------------------------------------------------ */

    @Test
    void allowPropertiesInsert_permiteCodeNameYGrado() {
        AllowProperties allowProperties = service.allowPropertiesInsert();

        assertTrue(allowProperties.allowProperty("code"));
        assertTrue(allowProperties.allowProperty("name"));
        assertTrue(allowProperties.allowProperty("grado"));
    }

    @Test
    void allowPropertiesInsert_noPermitePropiedadesFueraDeLaLista() {
        AllowProperties allowProperties = service.allowPropertiesInsert();

        assertFalse(allowProperties.allowProperty("archived"));
        assertFalse(allowProperties.allowProperty("createdBy"));
    }

    @Test
    void allowPropertiesInsert_gradoLlevaMapaInternoVacio() {
        AllowProperties allowPropertiesGrado = service.allowPropertiesInsert().innerAllowProperties("grado");

        assertFalse(allowPropertiesGrado.allowProperty("code"));
        assertFalse(allowPropertiesGrado.allowProperty("name"));
    }

    /* ------------------------------------------------------------------ */
    /* allowPropertiesUpdate                                              */
    /* ------------------------------------------------------------------ */

    @Test
    void allowPropertiesUpdate_permiteLaMismaListaQueElAlta() {
        AllowProperties allowProperties = service.allowPropertiesUpdate();

        assertTrue(allowProperties.allowProperty("code"));
        assertTrue(allowProperties.allowProperty("name"));
        assertTrue(allowProperties.allowProperty("grado"));
        assertFalse(allowProperties.allowProperty("archived"));

        AllowProperties allowPropertiesGrado = allowProperties.innerAllowProperties("grado");
        assertFalse(allowPropertiesGrado.allowProperty("code"));
        assertFalse(allowPropertiesGrado.allowProperty("name"));
    }

}
