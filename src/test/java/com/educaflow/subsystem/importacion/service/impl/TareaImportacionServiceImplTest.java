package com.educaflow.subsystem.importacion.service.impl;

import com.axelor.db.Repository;
import com.educaflow.subsystem.importacion.db.TareaImportacion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class TareaImportacionServiceImplTest {

    private TareaImportacionServiceImpl service;
    private Repository<TareaImportacion> repository;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        repository = Mockito.mock(Repository.class);
        service = new TareaImportacionServiceImpl(TareaImportacion.class, repository);
    }

    @Test
    void update_lanzaUnsupportedOperationExceptionSinGuardarNada() {
        TareaImportacion tareaImportacion = new TareaImportacion();

        assertThrows(UnsupportedOperationException.class, () -> service.update(tareaImportacion, new TareaImportacion()));
        verify(repository, never()).save(any());
    }

    @Test
    void remove_lanzaUnsupportedOperationExceptionSinBorrarNada() {
        TareaImportacion tareaImportacion = new TareaImportacion();

        assertThrows(UnsupportedOperationException.class, () -> service.remove(tareaImportacion));
        verify(repository, never()).remove(any());
    }
}
