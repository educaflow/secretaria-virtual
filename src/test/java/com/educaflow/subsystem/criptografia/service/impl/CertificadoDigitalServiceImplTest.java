package com.educaflow.subsystem.criptografia.service.impl;

import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.meta.db.MetaFile;
import com.educaflow.base.infrastructure.criptografia.AlmacenClave;
import com.educaflow.base.infrastructure.criptografia.AlmacenClaveDispositivo;
import com.educaflow.base.infrastructure.criptografia.AlmacenClaveFichero;
import com.educaflow.base.util.MetaFileUtil;
import com.educaflow.subsystem.criptografia.db.Alias;
import com.educaflow.subsystem.criptografia.db.CertificadoDigital;
import com.educaflow.subsystem.criptografia.db.DispositivoCriptografico;
import com.educaflow.subsystem.criptografia.db.TipoUbicacionCertificado;
import com.educaflow.subsystem.criptografia.db.repo.CertificadoDigitalRepository;
import jakarta.validation.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CertificadoDigitalServiceImplTest {

    private static final String DNI = "85432016B";
    private static final String DNI_INVALIDO = "12345678A";
    private static final String RUTA_CLASSPATH_CERTIFICADO = "firma/mi_certificado.p12";
    private static final String CLAVE = "nadanada";
    private static final String CLAVE_TECLEADA_DISTINTA = "claveTecleadaDistinta";
    private static final String CLAVE_EN_BLANCO = "   ";
    private static final String CLAVE_SECRETA = "claveSecretaDePrueba";
    private static final String MENSAJE_NO_EXISTE_CERTIFICADO = "No existe certificado para el DNI: " + DNI;
    private static final String MENSAJE_PASSWORD_NULL = "El password no puede ser null";
    private static final String MENSAJE_DNI_NO_VALIDO = "El DNI no es válido";

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

    private CertificadoDigital certificadoClasspath(String password) {
        CertificadoDigital certificado = new CertificadoDigital();
        certificado.setDni(DNI);
        certificado.setTipoCertificado(TipoUbicacionCertificado.CLASSPATH);
        certificado.setRutaClasspath(RUTA_CLASSPATH_CERTIFICADO);
        certificado.setPassword(password);
        certificado.setEnabled(Boolean.TRUE);
        return certificado;
    }

    private CertificadoDigital certificadoFicheroBd(MetaFile fichero, String password) {
        CertificadoDigital certificado = new CertificadoDigital();
        certificado.setDni(DNI);
        certificado.setTipoCertificado(TipoUbicacionCertificado.FICHERO_BD);
        certificado.setFichero(fichero);
        certificado.setPassword(password);
        certificado.setEnabled(Boolean.TRUE);
        return certificado;
    }

    private CertificadoDigital certificadoSistemaArchivos(Path rutaCertificado, String password) {
        CertificadoDigital certificado = new CertificadoDigital();
        certificado.setDni(DNI);
        certificado.setTipoCertificado(TipoUbicacionCertificado.SISTEMA_ARCHIVOS);
        certificado.setRutaSistemaArchivos(rutaCertificado.toString());
        certificado.setPassword(password);
        certificado.setEnabled(Boolean.TRUE);
        return certificado;
    }

    /**
     * Comprueba que el almacén es un {@link AlmacenClaveFichero} con la clave esperada y
     * cierra el {@code InputStream} del certificado, que el servicio abre sobre un recurso
     * real (classpath o sistema de archivos) y nadie más cerraría.
     */
    private void assertAlmacenFicheroConClave(AlmacenClave almacenClave, String claveEsperada) throws IOException {
        AlmacenClaveFichero almacenClaveFichero = assertInstanceOf(AlmacenClaveFichero.class, almacenClave);
        try (InputStream contenidoCertificado = almacenClaveFichero.getFileCertificate()) {
            assertNotNull(contenidoCertificado);
            assertEquals(claveEsperada, almacenClaveFichero.getPassword());
        }
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

    /* ------------------------------------------------------------------ */
    /* getAlmacenClaveByDni(dni, claveAcceso)                             */
    /* ------------------------------------------------------------------ */

    @Test
    void getAlmacenClaveByDni_ficheroConClaveGuardada_usaLaGuardadaEIgnoraLaTecleada() throws IOException {
        when(repository.findByDni(DNI)).thenReturn(certificadoClasspath(CLAVE));

        AlmacenClave almacenClave = service.getAlmacenClaveByDni(DNI, CLAVE_TECLEADA_DISTINTA);

        assertAlmacenFicheroConClave(almacenClave, CLAVE);
    }

    @Test
    void getAlmacenClaveByDni_ficheroSinClaveGuardada_usaLaClaveTecleada() throws IOException {
        when(repository.findByDni(DNI)).thenReturn(certificadoClasspath(null));

        AlmacenClave almacenClave = service.getAlmacenClaveByDni(DNI, CLAVE);

        assertAlmacenFicheroConClave(almacenClave, CLAVE);
    }

    @Test
    void getAlmacenClaveByDni_ficheroConClaveGuardadaEnBlanco_usaLaClaveTecleada() throws IOException {
        when(repository.findByDni(DNI)).thenReturn(certificadoClasspath(CLAVE_EN_BLANCO));

        AlmacenClave almacenClave = service.getAlmacenClaveByDni(DNI, CLAVE);

        assertAlmacenFicheroConClave(almacenClave, CLAVE);
    }

    @Test
    void getAlmacenClaveByDni_ficheroSinClaveGuardadaNiTecleada_lanzaExcepcion() {
        when(repository.findByDni(DNI)).thenReturn(certificadoClasspath(null));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.getAlmacenClaveByDni(DNI, null));

        assertEquals(MENSAJE_PASSWORD_NULL, ex.getMessage());
    }

    @Test
    void getAlmacenClaveByDni_ficheroEnBaseDeDatos_descargaElContenidoYUsaLaClaveEfectiva() throws IOException {
        MetaFile fichero = new MetaFile();
        when(repository.findByDni(DNI)).thenReturn(certificadoFicheroBd(fichero, null));

        try (MockedStatic<MetaFileUtil> metaFileUtil = Mockito.mockStatic(MetaFileUtil.class)) {
            metaFileUtil.when(() -> MetaFileUtil.downloadContent(fichero)).thenReturn(new byte[]{1, 2, 3});

            AlmacenClave almacenClave = service.getAlmacenClaveByDni(DNI, CLAVE);

            assertAlmacenFicheroConClave(almacenClave, CLAVE);
            metaFileUtil.verify(() -> MetaFileUtil.downloadContent(fichero));
        }
    }

    @Test
    void getAlmacenClaveByDni_sistemaDeArchivos_usaLaClaveEfectiva(@TempDir Path carpetaTemporal) throws IOException {
        Path rutaCertificado = Files.write(carpetaTemporal.resolve("certificado.p12"), new byte[]{1, 2, 3});
        when(repository.findByDni(DNI)).thenReturn(certificadoSistemaArchivos(rutaCertificado, null));

        AlmacenClave almacenClave = service.getAlmacenClaveByDni(DNI, CLAVE);

        assertAlmacenFicheroConClave(almacenClave, CLAVE);
    }

    @Test
    void getAlmacenClaveByDni_dispositivoPkcs11_descartaLaClaveTecleada() {
        CertificadoDigital certificado = certificadoDispositivoPkcs11();
        certificado.setEnabled(Boolean.TRUE);
        when(repository.findByDni(DNI)).thenReturn(certificado);

        AlmacenClave almacenClave = service.getAlmacenClaveByDni(DNI, "pinTecleado");

        AlmacenClaveDispositivo almacenClaveDispositivo = assertInstanceOf(AlmacenClaveDispositivo.class, almacenClave);
        assertEquals(1, almacenClaveDispositivo.getSlot());
        assertEquals("certificado-centro", almacenClaveDispositivo.getAlias());
    }

    @Test
    void getAlmacenClaveByDni_sinCertificadoParaElDni_lanzaExcepcionNoExisteCertificado() {
        when(repository.findByDni(DNI)).thenReturn(null);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.getAlmacenClaveByDni(DNI, CLAVE));

        assertEquals(MENSAJE_NO_EXISTE_CERTIFICADO, ex.getMessage());
    }

    @Test
    void getAlmacenClaveByDni_certificadoDeshabilitado_lanzaExcepcionNoExisteCertificado() {
        CertificadoDigital certificado = certificadoClasspath(CLAVE);
        certificado.setEnabled(Boolean.FALSE);
        when(repository.findByDni(DNI)).thenReturn(certificado);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.getAlmacenClaveByDni(DNI, CLAVE));

        assertEquals(MENSAJE_NO_EXISTE_CERTIFICADO, ex.getMessage());
    }

    @Test
    void getAlmacenClaveByDni_dniInvalido_lanzaValidationExceptionYNoConsultaElRepositorio() {
        ValidationException ex = assertThrows(ValidationException.class,
                () -> service.getAlmacenClaveByDni(DNI_INVALIDO, CLAVE));

        assertTrue(ex.getMessage().contains(MENSAJE_DNI_NO_VALIDO),
                () -> "El mensaje de la excepción no contiene «" + MENSAJE_DNI_NO_VALIDO + "»: " + ex.getMessage());
        verify(repository, never()).findByDni(any());
    }

    /* ------------------------------------------------------------------ */
    /* getAlmacenClaveByDni(dni) — delegación                             */
    /* ------------------------------------------------------------------ */

    @Test
    void getAlmacenClaveByDniUnArgumento_certificadoConClaveGuardada_devuelveElMismoResultadoQueAntes() throws IOException {
        when(repository.findByDni(DNI)).thenReturn(certificadoClasspath(CLAVE));

        AlmacenClave almacenClave = service.getAlmacenClaveByDni(DNI);

        assertAlmacenFicheroConClave(almacenClave, CLAVE);
    }

    @Test
    void getAlmacenClaveByDniUnArgumento_dispositivoPkcs11Habilitado_devuelveAlmacenClaveDispositivo() {
        CertificadoDigital certificado = certificadoDispositivoPkcs11();
        certificado.setEnabled(Boolean.TRUE);
        when(repository.findByDni(DNI)).thenReturn(certificado);

        AlmacenClave almacenClave = service.getAlmacenClaveByDni(DNI);

        assertInstanceOf(AlmacenClaveDispositivo.class, almacenClave);
    }

    @Test
    void getAlmacenClaveByDniUnArgumento_certificadoSinClaveGuardada_lanzaExcepcion() {
        when(repository.findByDni(DNI)).thenReturn(certificadoClasspath(null));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.getAlmacenClaveByDni(DNI));

        assertEquals(MENSAJE_PASSWORD_NULL, ex.getMessage());
    }

    /* ------------------------------------------------------------------ */
    /* validateGetAlmacenClaveByDni(dni, claveAcceso)                     */
    /* ------------------------------------------------------------------ */

    @Test
    void validateGetAlmacenClaveByDni_dniValidoYClaveTecleada_devuelveOptionalVacio() {
        Optional<BusinessMessages> messages = service.validateGetAlmacenClaveByDni(DNI, CLAVE);

        assertEquals(Optional.empty(), messages);
    }

    @Test
    void validateGetAlmacenClaveByDni_claveAccesoNula_devuelveOptionalVacio() {
        Optional<BusinessMessages> messages = service.validateGetAlmacenClaveByDni(DNI, null);

        assertEquals(Optional.empty(), messages);
    }

    @Test
    void validateGetAlmacenClaveByDni_dniInvalido_devuelveMensajeElDniNoEsValido() {
        Optional<BusinessMessages> messages = service.validateGetAlmacenClaveByDni(DNI_INVALIDO, CLAVE);

        assertTrue(messages.isPresent(), "Se esperaba al menos un mensaje de validación para un DNI inválido");
        assertTrue(messages.get().stream()
                        .anyMatch(message -> "dni".equals(message.getFieldName())
                                && MENSAJE_DNI_NO_VALIDO.equals(message.getMessage())),
                () -> "No hay ningún mensaje del campo «dni» con el texto «" + MENSAJE_DNI_NO_VALIDO
                        + "». Mensajes obtenidos: " + messages.get());
    }

    @Test
    void validateGetAlmacenClaveByDni_dniInvalido_noIncluyeLaClaveEnNingunMensaje() {
        String primeraMitadDeLaClave = CLAVE_SECRETA.substring(0, CLAVE_SECRETA.length() / 2);
        String segundaMitadDeLaClave = CLAVE_SECRETA.substring(CLAVE_SECRETA.length() / 2);

        Optional<BusinessMessages> messages = service.validateGetAlmacenClaveByDni(DNI_INVALIDO, CLAVE_SECRETA);

        assertTrue(messages.isPresent(), "Se esperaba al menos un mensaje de validación para un DNI inválido");
        String textoMensajes = messages.get().toString();
        assertFalse(textoMensajes.contains(CLAVE_SECRETA),
                () -> "Los mensajes de validación filtran la clave completa: " + textoMensajes);
        assertFalse(textoMensajes.contains(primeraMitadDeLaClave),
                () -> "Los mensajes de validación filtran «" + primeraMitadDeLaClave + "»: " + textoMensajes);
        assertFalse(textoMensajes.contains(segundaMitadDeLaClave),
                () -> "Los mensajes de validación filtran «" + segundaMitadDeLaClave + "»: " + textoMensajes);
    }
}
