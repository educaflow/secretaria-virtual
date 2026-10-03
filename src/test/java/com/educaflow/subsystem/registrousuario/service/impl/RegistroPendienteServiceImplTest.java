package com.educaflow.subsystem.registrousuario.service.impl;

import com.axelor.db.Repository;
import com.axelor.db.modelservice.AllowProperties;
import com.educaflow.subsystem.registrousuario.db.RegistroPendiente;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertFalse;

class RegistroPendienteServiceImplTest {

    private RegistroPendienteServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new RegistroPendienteServiceImpl(RegistroPendiente.class, Mockito.mock(Repository.class));
    }

    @Test
    void allowPropertiesInsert_noPermiteNingunCampo() {
        assertNoPermiteNingunCampo(service.allowPropertiesInsert());
    }

    @Test
    void allowPropertiesUpdate_noPermiteNingunCampo() {
        assertNoPermiteNingunCampo(service.allowPropertiesUpdate());
    }

    private static void assertNoPermiteNingunCampo(AllowProperties allowProperties) {
        assertFalse(allowProperties.allowProperty("email"));
        assertFalse(allowProperties.allowProperty("dni"));
        assertFalse(allowProperties.allowProperty("codigo"));
        assertFalse(allowProperties.allowProperty("token"));
        assertFalse(allowProperties.allowProperty("verificado"));
    }
}
