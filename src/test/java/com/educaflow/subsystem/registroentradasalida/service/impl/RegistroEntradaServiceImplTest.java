package com.educaflow.subsystem.registroentradasalida.service.impl;

import com.axelor.db.Repository;
import com.educaflow.subsystem.registroentradasalida.db.RegistroEntrada;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class RegistroEntradaServiceImplTest {

    private RegistroEntradaServiceImpl service;
    private Repository<RegistroEntrada> repository;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        repository = Mockito.mock(Repository.class);
        service = new RegistroEntradaServiceImpl(RegistroEntrada.class, repository);
    }

    @Test
    void validateInsert_siempreRechaza() {
        assertTrue(service.validateInsert(new RegistroEntrada()).isPresent());
    }

    @Test
    void insert_lanzaUnsupportedOperationExceptionSinGuardarNada() {
        RegistroEntrada registroEntrada = new RegistroEntrada();

        assertThrows(UnsupportedOperationException.class, () -> service.insert(registroEntrada));
        verify(repository, never()).save(any());
    }

    @Test
    void validateUpdate_siempreRechaza() {
        assertTrue(service.validateUpdate(new RegistroEntrada(), new RegistroEntrada()).isPresent());
    }

    @Test
    void update_lanzaUnsupportedOperationExceptionSinGuardarNada() {
        RegistroEntrada registroEntrada = new RegistroEntrada();

        assertThrows(UnsupportedOperationException.class, () -> service.update(registroEntrada, new RegistroEntrada()));
        verify(repository, never()).save(any());
    }

    @Test
    void validateRemove_siempreRechaza() {
        assertTrue(service.validateRemove(new RegistroEntrada()).isPresent());
    }

    @Test
    void remove_lanzaUnsupportedOperationExceptionSinBorrarNada() {
        RegistroEntrada registroEntrada = new RegistroEntrada();

        assertThrows(UnsupportedOperationException.class, () -> service.remove(registroEntrada));
        verify(repository, never()).remove(any());
    }
}
