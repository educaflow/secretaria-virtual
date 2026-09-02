package com.educaflow.base.infrastructure.criptografia;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlmacenClaveFicheroTest {

    /** Certificado de pruebas compartido con {@code DocumentoPdfImplITextTest}. */
    private static final String FILE_CERTIFICADO = "/com/educaflow/base/infrastructure/pdf/mi_certificado_password_nadanada.p12";
    private static final String PASSWORD_CERTIFICADO = "nadanada";

    private static InputStream certificado() {
        return AlmacenClaveFicheroTest.class.getResourceAsStream(FILE_CERTIFICADO);
    }

    /* ------------------------------------------------------------------ */
    /* isPasswordValid                                                     */
    /* ------------------------------------------------------------------ */

    @Test
    void isPasswordValid_passwordCorrecto_devuelveTrue() {
        AlmacenClaveFichero almacenClaveFichero = new AlmacenClaveFichero(certificado(), PASSWORD_CERTIFICADO);

        assertTrue(almacenClaveFichero.isPasswordValid());
    }

    @Test
    void isPasswordValid_passwordIncorrecto_devuelveFalse() {
        AlmacenClaveFichero almacenClaveFichero = new AlmacenClaveFichero(certificado(), "passwordQueNoEs");

        assertFalse(almacenClaveFichero.isPasswordValid());
    }

    @Test
    void isPasswordValid_ficheroQueNoEsUnPkcs12_lanzaExcepcion() {
        InputStream contenidoInvalido = new ByteArrayInputStream("esto no es un PKCS#12".getBytes(StandardCharsets.UTF_8));
        AlmacenClaveFichero almacenClaveFichero = new AlmacenClaveFichero(contenidoInvalido, PASSWORD_CERTIFICADO);

        assertThrows(RuntimeException.class, () -> almacenClaveFichero.isPasswordValid());
    }

    @Test
    void isPasswordValid_llamadasRepetidas_noAgotaElCertificado() {
        AlmacenClaveFichero almacenClaveFichero = new AlmacenClaveFichero(certificado(), PASSWORD_CERTIFICADO);

        assertTrue(almacenClaveFichero.isPasswordValid());
        assertTrue(almacenClaveFichero.isPasswordValid());
    }

    /* ------------------------------------------------------------------ */
    /* getFileCertificate                                                  */
    /* ------------------------------------------------------------------ */

    @Test
    void getFileCertificate_variasLlamadas_devuelveUnStreamNuevoConElMismoContenido() throws IOException {
        byte[] contenidoOriginal;
        try (InputStream inputStream = certificado()) {
            contenidoOriginal = inputStream.readAllBytes();
        }
        AlmacenClaveFichero almacenClaveFichero = new AlmacenClaveFichero(new ByteArrayInputStream(contenidoOriginal), PASSWORD_CERTIFICADO);

        InputStream primeraLectura = almacenClaveFichero.getFileCertificate();
        InputStream segundaLectura = almacenClaveFichero.getFileCertificate();

        assertNotSame(primeraLectura, segundaLectura);
        assertArrayEquals(contenidoOriginal, primeraLectura.readAllBytes());
        assertArrayEquals(contenidoOriginal, segundaLectura.readAllBytes());
    }

    @Test
    void getFileCertificate_despuesDeIsPasswordValid_sigueDevolviendoElContenido() throws IOException {
        AlmacenClaveFichero almacenClaveFichero = new AlmacenClaveFichero(certificado(), PASSWORD_CERTIFICADO);

        almacenClaveFichero.isPasswordValid();

        try (InputStream inputStream = almacenClaveFichero.getFileCertificate()) {
            assertTrue(inputStream.readAllBytes().length > 0);
        }
    }

    /* ------------------------------------------------------------------ */
    /* Constructor                                                         */
    /* ------------------------------------------------------------------ */

    @Test
    void constructor_ficheroNull_lanzaExcepcion() {
        assertThrows(RuntimeException.class, () -> new AlmacenClaveFichero(null, PASSWORD_CERTIFICADO));
    }

    @Test
    void constructor_passwordNull_lanzaExcepcion() {
        assertThrows(RuntimeException.class, () -> new AlmacenClaveFichero(certificado(), null));
    }

}
