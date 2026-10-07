package com.educaflow.base.infrastructure.criptografia.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.educaflow.base.infrastructure.criptografia.EntornoCriptograficoDePruebas;
import com.educaflow.base.infrastructure.criptografia.TipoCertificado;
import com.educaflow.base.infrastructure.criptografia.TipoEmisorCertificado;
import com.educaflow.datademo.UsuariosDemo;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Los certificados que emite la CA FALSA de demo (firma/demo/) los entiende la aplicación como certificados ACCV
 * de usuario final, y solo son de confianza con el almacén de demo (truststore-demo.jks), no con el oficial.
 */
class DatosCertificadoImplCaDemoTest {

    private static final String CLAVE_CERTIFICADOS_DEMO = "demo1234";
    private static final String PASSWORD_ALMACEN = "s3cr3T";
    private static final String RUTA_ALMACEN_DEMO = "firma/demo/almacen/truststore-demo.jks";
    private static final String RUTA_ALMACEN_OFICIAL = "firma/AlmacenCertificadosConfiables/truststore.jks";
    private static final String EMAIL_USUARIO_DEMO = "alumno1@mislata.es";
    private static final String RUTA_CERTIFICADO_TEST = "firma/test/test1.p12";
    private static final String RUTA_README_CERTIFICADOS_TEST = "firma/test/README.txt";

    private static KeyStore almacenDemo;
    private static KeyStore almacenOficial;

    @BeforeAll
    static void cargarAlmacenes() throws Exception {
        // Las CRL las lee DatosCertificadoImpl del estado estático del entorno criptográfico, compartido por toda la JVM
        EntornoCriptograficoDePruebas.configurar();
        almacenDemo = cargarAlmacen(RUTA_ALMACEN_DEMO, "JKS", PASSWORD_ALMACEN);
        almacenOficial = cargarAlmacen(RUTA_ALMACEN_OFICIAL, "JKS", PASSWORD_ALMACEN);
    }

    @Test
    void certificadoDeUsuarioDemo_esAccvDeUsuarioFinalConLosDatosDelUsuario() throws Exception {
        UsuariosDemo.UsuarioDemo usuario = UsuariosDemo.getUsuarioDemo(EMAIL_USUARIO_DEMO);
        X509Certificate certificado = certificadoDe("firma/demo/certificados/" + usuario.documento() + ".p12");

        DatosCertificadoImpl datos = new DatosCertificadoImpl(certificado, almacenDemo);

        assertEquals(TipoEmisorCertificado.ACCV, datos.getTipoEmisorCertificado());
        assertEquals(TipoCertificado.USUARIO_FINAL, datos.getTipoCertificado());
        assertEquals(usuario.documento(), datos.getDNI());
        assertEquals(usuario.nombre(), datos.getNombre());
        assertEquals(usuario.apellidos(), datos.getApellidos());
    }

    @Test
    void certificadoDeUsuarioDemo_soloEsDeConfianzaConElAlmacenDeDemo() throws Exception {
        X509Certificate certificado = certificadoDe("firma/demo/certificados/" + UsuariosDemo.getUsuarioDemo(EMAIL_USUARIO_DEMO).documento() + ".p12");

        assertTrue(new DatosCertificadoImpl(certificado, almacenDemo).isValidoEnListaCertificadosConfiables());
        assertFalse(new DatosCertificadoImpl(certificado, almacenOficial).isValidoEnListaCertificadosConfiables());
    }

    @Test
    void certificadoDeTest_esAccvDeUsuarioFinalConSusDatos() throws Exception {
        X509Certificate certificado = certificadoDe(RUTA_CERTIFICADO_TEST);

        DatosCertificadoImpl datos = new DatosCertificadoImpl(certificado, almacenDemo);

        assertEquals(TipoEmisorCertificado.ACCV, datos.getTipoEmisorCertificado());
        assertEquals(TipoCertificado.USUARIO_FINAL, datos.getTipoCertificado());
        assertEquals(dniCertificadoTest("test1.p12"), datos.getDNI());
        assertEquals("test1", datos.getNombre());
        assertEquals("Certificado Test", datos.getApellidos());
    }

    @Test
    void certificadoDeTest_soloEsDeConfianzaConElAlmacenDeDemo() throws Exception {
        X509Certificate certificado = certificadoDe(RUTA_CERTIFICADO_TEST);

        assertTrue(new DatosCertificadoImpl(certificado, almacenDemo).isValidoEnListaCertificadosConfiables());
        assertFalse(new DatosCertificadoImpl(certificado, almacenOficial).isValidoEnListaCertificadosConfiables());
    }

    private static X509Certificate certificadoDe(String rutaClasspath) throws Exception {
        KeyStore p12 = cargarAlmacen(rutaClasspath, "PKCS12", CLAVE_CERTIFICADOS_DEMO);
        String alias = p12.aliases().nextElement();
        return (X509Certificate) p12.getCertificate(alias);
    }

    private static KeyStore cargarAlmacen(String rutaClasspath, String tipo, String password) throws Exception {
        try (InputStream inputStream = DatosCertificadoImplCaDemoTest.class.getClassLoader().getResourceAsStream(rutaClasspath)) {
            assertNotNull(inputStream, "No está en el classpath: " + rutaClasspath);
            KeyStore keyStore = KeyStore.getInstance(tipo);
            keyStore.load(inputStream, password.toCharArray());
            return keyStore;
        }
    }

    /**
     * El DNI de un certificado de test es el que apuntó crear_certificado_test.sh en el README de los certificados.
     */
    private static String dniCertificadoTest(String fichero) throws Exception {
        try (InputStream inputStream = DatosCertificadoImplCaDemoTest.class.getClassLoader().getResourceAsStream(RUTA_README_CERTIFICADOS_TEST)) {
            assertNotNull(inputStream, "No está en el classpath: " + RUTA_README_CERTIFICADOS_TEST);
            String readme = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            Matcher matcher = Pattern.compile("^" + Pattern.quote(fichero) + "\\s+DNI=(\\S+)", Pattern.MULTILINE).matcher(readme);
            assertTrue(matcher.find(), "El README de los certificados de test no tiene la línea de " + fichero);
            return matcher.group(1);
        }
    }
}
