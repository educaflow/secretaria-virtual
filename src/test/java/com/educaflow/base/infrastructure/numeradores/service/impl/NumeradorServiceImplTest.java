package com.educaflow.base.infrastructure.numeradores.service.impl;

import com.axelor.db.Repository;
import com.axelor.db.modelservice.AllowProperties;
import com.educaflow.base.infrastructure.numeradores.db.Numerador;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class NumeradorServiceImplTest {

    private NumeradorServiceImpl service;
    private Repository<Numerador> repository;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        repository = Mockito.mock(Repository.class);
        service = new NumeradorServiceImpl(Numerador.class, repository);
    }

    @Test
    void validateRemove_siempreRechaza() {
        assertTrue(service.validateRemove(new Numerador()).isPresent());
    }

    @Test
    void remove_lanzaUnsupportedOperationExceptionSinBorrarNada() {
        Numerador numerador = new Numerador();

        assertThrows(UnsupportedOperationException.class, () -> service.remove(numerador));
        verify(repository, never()).remove(any());
    }

    @Test
    void validateInsert_siempreRechaza() {
        assertTrue(service.validateInsert(new Numerador()).isPresent());
    }

    @Test
    void insert_lanzaUnsupportedOperationExceptionSinGuardarNada() {
        Numerador numerador = new Numerador();

        assertThrows(UnsupportedOperationException.class, () -> service.insert(numerador));
        verify(repository, never()).save(any());
    }

    @Test
    void validateUpdate_siempreRechaza() {
        assertTrue(service.validateUpdate(new Numerador(), new Numerador()).isPresent());
    }

    @Test
    void update_lanzaUnsupportedOperationExceptionSinGuardarNada() {
        Numerador numerador = new Numerador();

        assertThrows(UnsupportedOperationException.class, () -> service.update(numerador, new Numerador()));
        verify(repository, never()).save(any());
    }

    @Test
    void allowPropertiesInsert_noPermiteNingunCampo() {
        AllowProperties allowProperties = service.allowPropertiesInsert();

        assertFalse(allowProperties.allowProperty("tipoNumerador"));
        assertFalse(allowProperties.allowProperty("centro"));
        assertFalse(allowProperties.allowProperty("anyo"));
        assertFalse(allowProperties.allowProperty("ultimoNumero"));
    }

    @Test
    void allowPropertiesUpdate_noPermiteNingunCampo() {
        AllowProperties allowProperties = service.allowPropertiesUpdate();

        assertFalse(allowProperties.allowProperty("tipoNumerador"));
        assertFalse(allowProperties.allowProperty("centro"));
        assertFalse(allowProperties.allowProperty("anyo"));
        assertFalse(allowProperties.allowProperty("ultimoNumero"));
    }
}
