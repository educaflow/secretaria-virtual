package com.educaflow.system.gruposnotas.service.impl;

import com.axelor.db.Repository;
import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.i18n.I18n;
import com.educaflow.subsystem.sistemaeducativo.db.Modulo;
import com.educaflow.system.gruposnotas.db.Grupo;
import com.educaflow.system.gruposnotas.db.ModuloGrupo;
import com.educaflow.system.gruposnotas.service.ModuloGrupoInsertDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ModuloGrupoServiceImplTest {

    @SuppressWarnings("unchecked")
    private final Repository<ModuloGrupo> repository = Mockito.mock(Repository.class);

    private ModuloGrupoServiceImpl service;

    private MockedStatic<I18n> i18nMock;

    @BeforeEach
    void setUp() {
        service = new ModuloGrupoServiceImpl(ModuloGrupo.class, repository);

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
    /* validateInsert                                                     */
    /* ------------------------------------------------------------------ */

    @Test
    void validateInsert_dtoCompleto_devuelveVacio() {
        ModuloGrupoInsertDTO dto = new ModuloGrupoInsertDTO(new Grupo(), new Modulo());

        Optional<BusinessMessages> resultado = service.validateInsert(dto);

        assertTrue(resultado.isEmpty());
    }

    @Test
    void validateInsert_grupoNulo_devuelveError() {
        ModuloGrupoInsertDTO dto = new ModuloGrupoInsertDTO(null, new Modulo());

        Optional<BusinessMessages> resultado = service.validateInsert(dto);

        assertTrue(resultado.isPresent());
    }

    @Test
    void validateInsert_moduloNulo_devuelveError() {
        ModuloGrupoInsertDTO dto = new ModuloGrupoInsertDTO(new Grupo(), null);

        Optional<BusinessMessages> resultado = service.validateInsert(dto);

        assertTrue(resultado.isPresent());
    }

    /* ------------------------------------------------------------------ */
    /* insert                                                             */
    /* ------------------------------------------------------------------ */

    @Test
    void insert_dtoValido_construyeYGuardaModuloGrupo() {
        Grupo grupo = new Grupo();
        Modulo modulo = new Modulo();
        ModuloGrupoInsertDTO dto = new ModuloGrupoInsertDTO(grupo, modulo);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ModuloGrupo resultado = service.insert(dto);

        assertSame(grupo, resultado.getGrupo());
        assertSame(modulo, resultado.getModulo());
        verify(repository).save(any());
    }
}
