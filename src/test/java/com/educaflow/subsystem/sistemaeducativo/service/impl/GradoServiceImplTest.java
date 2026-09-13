package com.educaflow.subsystem.sistemaeducativo.service.impl;

import com.axelor.db.modelservice.AllowProperties;
import com.educaflow.subsystem.sistemaeducativo.db.Grado;
import com.educaflow.subsystem.sistemaeducativo.db.repo.GradoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class GradoServiceImplTest {

    private GradoRepository repository;
    private GradoServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(GradoRepository.class);
        service = new GradoServiceImpl(Grado.class, repository);
    }

    /* ------------------------------------------------------------------ */
    /* allowPropertiesInsert                                              */
    /* ------------------------------------------------------------------ */

    @Test
    void allowPropertiesInsert_permiteSoloCodeYName() {
        AllowProperties allowProperties = service.allowPropertiesInsert();

        assertTrue(allowProperties.allowProperty("code"));
        assertTrue(allowProperties.allowProperty("name"));
    }

    @Test
    void allowPropertiesInsert_noPermiteNivelesNiAdmiteNivel() {
        AllowProperties allowProperties = service.allowPropertiesInsert();

        assertFalse(allowProperties.allowProperty("niveles"));
        assertFalse(allowProperties.allowProperty("admiteNivel"));
        assertFalse(allowProperties.allowProperty("archived"));
    }

    /* ------------------------------------------------------------------ */
    /* allowPropertiesUpdate                                              */
    /* ------------------------------------------------------------------ */

    @Test
    void allowPropertiesUpdate_permiteLaMismaListaQueElAlta() {
        AllowProperties allowProperties = service.allowPropertiesUpdate();

        assertTrue(allowProperties.allowProperty("code"));
        assertTrue(allowProperties.allowProperty("name"));
        assertFalse(allowProperties.allowProperty("niveles"));
        assertFalse(allowProperties.allowProperty("admiteNivel"));
        assertFalse(allowProperties.allowProperty("archived"));
    }

}
