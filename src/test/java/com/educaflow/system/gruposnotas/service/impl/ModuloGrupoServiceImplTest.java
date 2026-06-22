package com.educaflow.system.gruposnotas.service.impl;

import com.axelor.db.Query;
import com.axelor.db.Repository;
import com.axelor.db.modelservice.AllowProperties;
import com.axelor.db.modelservice.BusinessMessages;
import com.educaflow.subsystem.sistemaeducativo.db.Modulo;
import com.educaflow.system.gruposnotas.db.Grupo;
import com.educaflow.system.gruposnotas.db.ModuloGrupo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

/**
 * Tests unitarios de {@link ModuloGrupoServiceImpl}.
 *
 * <p>El servicio extiende {@code DefaultModelService<ModuloGrupo>} y se construye con
 * {@code (Class<ModuloGrupo>, Repository<ModuloGrupo>)}. La unicidad módulo–grupo se
 * comprueba con la cadena {@code repository.all().filter(...).bind(...).count()}, que se
 * mockea paso a paso sobre un mock de {@link Query}.</p>
 */
@ExtendWith(MockitoExtension.class)
class ModuloGrupoServiceImplTest {

    @Mock
    private Repository<ModuloGrupo> repository;

    @Mock
    private Query<ModuloGrupo> query;

    private ModuloGrupoServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ModuloGrupoServiceImpl(ModuloGrupo.class, repository);
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    /** Programa la cadena {@code all().filter().bind().bind().count()} para devolver {@code count}. */
    private void programarConteoExistencia(long count) {
        lenient().when(repository.all()).thenReturn(query);
        lenient().when(query.filter(anyString())).thenReturn(query);
        lenient().when(query.bind(anyString(), any())).thenReturn(query);
        lenient().when(query.count()).thenReturn(count);
    }

    private static ModuloGrupo moduloGrupo() {
        ModuloGrupo moduloGrupo = new ModuloGrupo();
        moduloGrupo.setGrupo(new Grupo());
        moduloGrupo.setModulo(new Modulo());
        return moduloGrupo;
    }

    /** Texto del único {@link com.axelor.db.modelservice.BusinessMessage} presente. */
    private static String mensaje(Optional<BusinessMessages> resultado) {
        assertTrue(resultado.isPresent(), "Se esperaba un BusinessMessages presente");
        assertEquals(1, resultado.get().size(), "Se esperaba exactamente un mensaje");
        return resultado.get().get(0).getMessage();
    }

    // =====================================================================
    // validateInsert
    // =====================================================================

    @Test
    void validateInsert_moduloNoRepetido_devuelveOptionalVacio() {
        programarConteoExistencia(0L);

        Optional<BusinessMessages> resultado = service.validateInsert(moduloGrupo());

        assertTrue(resultado.isEmpty());
    }

    @Test
    void validateInsert_moduloDuplicado_devuelveMensajeModuloYaEnGrupo() {
        programarConteoExistencia(1L);

        Optional<BusinessMessages> resultado = service.validateInsert(moduloGrupo());

        assertEquals("El módulo ya está en el grupo.", mensaje(resultado));
    }

    // =====================================================================
    // update
    // =====================================================================

    @Test
    void update_siempre_lanzaUnsupportedOperationException() {
        ModuloGrupo moduloGrupo = moduloGrupo();
        ModuloGrupo original = moduloGrupo();

        assertThrows(UnsupportedOperationException.class,
                () -> service.update(moduloGrupo, original));
    }

    // =====================================================================
    // allowProperties*
    // =====================================================================

    @Test
    void allowPropertiesInsert_denyAll() {
        AllowProperties allow = service.allowPropertiesInsert();

        assertFalse(allow.allowProperty("grupo"));
        assertFalse(allow.allowProperty("modulo"));
        assertFalse(allow.allowProperty("id"));
    }

    @Test
    void allowPropertiesUpdate_denyAll() {
        AllowProperties allow = service.allowPropertiesUpdate();

        assertFalse(allow.allowProperty("grupo"));
        assertFalse(allow.allowProperty("modulo"));
        assertFalse(allow.allowProperty("id"));
    }
}
