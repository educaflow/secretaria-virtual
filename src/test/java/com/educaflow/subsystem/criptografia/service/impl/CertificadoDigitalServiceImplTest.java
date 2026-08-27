package com.educaflow.subsystem.criptografia.service.impl;

import com.educaflow.base.infrastructure.criptografia.AlmacenClave;
import com.educaflow.base.infrastructure.criptografia.AlmacenClaveDispositivo;
import com.educaflow.subsystem.criptografia.db.Alias;
import com.educaflow.subsystem.criptografia.db.CertificadoDigital;
import com.educaflow.subsystem.criptografia.db.DispositivoCriptografico;
import com.educaflow.subsystem.criptografia.db.TipoUbicacionCertificado;
import com.educaflow.subsystem.criptografia.db.repo.CertificadoDigitalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CertificadoDigitalServiceImplTest {

    private static final String DNI = "85432016B";
    private static final String MENSAJE_NO_EXISTE_CERTIFICADO = "No existe certificado para el DNI: " + DNI;

    private CertificadoDigitalRepository repository;
    private CertificadoDigitalServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(CertificadoDigitalRepository.class);
        service = new CertificadoDigitalServiceImpl(CertificadoDigital.class, repository);
    }

    /* ------------------------------------------------------------------ */
    /* Helpers                                                            */
    /* ------------------------------------------------------------------ */

    private CertificadoDigital certificadoDispositivoPkcs11() {
        DispositivoCriptografico dispositivoCriptografico = new DispositivoCriptografico();
        dispositivoCriptografico.setSlot(1);

        Alias alias = new Alias();
        alias.setName("certificado-centro");

        CertificadoDigital certificado = new CertificadoDigital();
        certificado.setDni(DNI);
        certificado.setTipoCertificado(TipoUbicacionCertificado.DISPOSITIVO_PKCS11);
        certificado.setDispositivoCriptografico(dispositivoCriptografico);
        certificado.setAlias(alias);
        return certificado;
    }

    /* ------------------------------------------------------------------ */
    /* getAlmacenClaveByDni                                               */
    /* ------------------------------------------------------------------ */

    @Test
    void getAlmacenClaveByDni_entradaDeshabilitada_lanzaMismaExcepcionQueInexistente() {
        CertificadoDigital certificado = certificadoDispositivoPkcs11();
        certificado.setEnabled(Boolean.FALSE);
        when(repository.findByDni(DNI)).thenReturn(certificado);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.getAlmacenClaveByDni(DNI));

        assertEquals(MENSAJE_NO_EXISTE_CERTIFICADO, ex.getMessage());
    }

    @Test
    void getAlmacenClaveByDni_entradaInexistente_lanzaExcepcionNoExisteCertificado() {
        when(repository.findByDni(DNI)).thenReturn(null);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.getAlmacenClaveByDni(DNI));

        assertEquals(MENSAJE_NO_EXISTE_CERTIFICADO, ex.getMessage());
    }

    @Test
    void getAlmacenClaveByDni_entradaHabilitada_devuelveAlmacenClave() {
        CertificadoDigital certificado = certificadoDispositivoPkcs11();
        certificado.setEnabled(Boolean.TRUE);
        when(repository.findByDni(DNI)).thenReturn(certificado);

        AlmacenClave almacenClave = service.getAlmacenClaveByDni(DNI);

        assertNotNull(almacenClave);
        assertInstanceOf(AlmacenClaveDispositivo.class, almacenClave);
    }

    @Test
    void getAlmacenClaveByDni_entradaConEnabledPorDefecto_devuelveAlmacenClave() {
        CertificadoDigital certificado = certificadoDispositivoPkcs11();
        when(repository.findByDni(DNI)).thenReturn(certificado);

        AlmacenClave almacenClave = service.getAlmacenClaveByDni(DNI);

        assertNotNull(almacenClave);
        assertInstanceOf(AlmacenClaveDispositivo.class, almacenClave);
    }
}
