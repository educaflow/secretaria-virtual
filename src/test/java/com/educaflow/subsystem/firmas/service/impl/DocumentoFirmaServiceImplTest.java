package com.educaflow.subsystem.firmas.service.impl;

import com.axelor.db.Repository;
import com.axelor.db.modelservice.AllowProperties;
import com.educaflow.subsystem.firmas.db.DocumentoFirma;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertFalse;

class DocumentoFirmaServiceImplTest {

    private DocumentoFirmaServiceImpl service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        service = new DocumentoFirmaServiceImpl(DocumentoFirma.class, Mockito.mock(Repository.class));
    }

    @Test
    void allowPropertiesInsert_noPermiteNingunCampo() {
        AllowProperties allowProperties = service.allowPropertiesInsert();

        assertFalse(allowProperties.allowProperty("tareaFirma"));
        assertFalse(allowProperties.allowProperty("documentoOriginal"));
        assertFalse(allowProperties.allowProperty("documentoFirmado"));
    }

    @Test
    void allowPropertiesUpdate_noPermiteNingunCampo() {
        AllowProperties allowProperties = service.allowPropertiesUpdate();

        assertFalse(allowProperties.allowProperty("tareaFirma"));
        assertFalse(allowProperties.allowProperty("documentoOriginal"));
        assertFalse(allowProperties.allowProperty("documentoFirmado"));
    }
}
