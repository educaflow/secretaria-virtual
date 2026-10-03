package com.educaflow.subsystem.criptografia.service.impl;

import com.axelor.db.Repository;
import com.axelor.db.modelservice.AllowProperties;
import com.educaflow.subsystem.criptografia.db.Alias;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class AliasServiceImplTest {

    private AliasServiceImpl service;
    private Repository<Alias> repository;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        repository = Mockito.mock(Repository.class);
        service = new AliasServiceImpl(Alias.class, repository);
    }

    @Test
    void validateInsert_siempreRechaza() {
        assertTrue(service.validateInsert(new Alias()).isPresent());
    }

    @Test
    void insert_lanzaUnsupportedOperationExceptionSinGuardarNada() {
        Alias alias = new Alias();

        assertThrows(UnsupportedOperationException.class, () -> service.insert(alias));
        verify(repository, never()).save(any());
    }

    @Test
    void validateUpdate_siempreRechaza() {
        assertTrue(service.validateUpdate(new Alias(), new Alias()).isPresent());
    }

    @Test
    void update_lanzaUnsupportedOperationExceptionSinGuardarNada() {
        Alias alias = new Alias();

        assertThrows(UnsupportedOperationException.class, () -> service.update(alias, new Alias()));
        verify(repository, never()).save(any());
    }

    @Test
    void allowPropertiesInsert_noPermiteNingunCampo() {
        AllowProperties allowProperties = service.allowPropertiesInsert();

        assertFalse(allowProperties.allowProperty("name"));
        assertFalse(allowProperties.allowProperty("dispositivoCriptografico"));
    }

    @Test
    void allowPropertiesUpdate_noPermiteNingunCampo() {
        AllowProperties allowProperties = service.allowPropertiesUpdate();

        assertFalse(allowProperties.allowProperty("name"));
        assertFalse(allowProperties.allowProperty("dispositivoCriptografico"));
    }
}
