package com.educaflow.secretariavirtual.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * El almacén de demo (truststore-demo.jks) y su lista de CRLs (crls-demo.xml) son de la CA FALSA de demo, cuyas claves
 * privadas están versionadas: solo se admiten en un entorno que carga los datos de demo, y nunca en producción.
 *
 * <p>La configuración privada de cada entorno está en la carpeta hermana {@code ../secretaria-virtual-private}, fuera
 * de este repositorio: si no está (p. ej. en CI), sus comprobaciones se omiten.
 */
class ConfiguracionEntornosTest {

    private static final String PROPIEDAD_ALMACEN = "entornoCriptografico.almacenCertificadosConfiables.path";
    private static final String PROPIEDAD_LISTA_CRLS = "entornoCriptografico.almacenCertificadosConfiables.pathListaCRLs";
    private static final String PROPIEDAD_DEMO_DATA = "data.import.demo-data";
    private static final String ALMACEN_OFICIAL = "firma/AlmacenCertificadosConfiables/truststore.jks";
    private static final String ALMACEN_DEMO = "firma/demo/almacen/truststore-demo.jks";
    private static final String LISTA_CRLS_OFICIAL = "firma/AlmacenCertificadosConfiables/crl/crls.xml";
    private static final String LISTA_CRLS_DEMO = "firma/demo/almacen/crls-demo.xml";
    private static final Path DIR_CONFIGURACION_PRIVADA = Path.of("..", "secretaria-virtual-private");

    @Test
    void configuracionComun_dejaLosValoresSeguros() throws Exception {
        Properties configuracion = new Properties();
        try (InputStream inputStream = ConfiguracionEntornosTest.class.getClassLoader().getResourceAsStream("axelor-config.properties")) {
            assertNotNull(inputStream, "No está axelor-config.properties en el classpath");
            configuracion.load(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        }

        assertEquals(ALMACEN_OFICIAL, propiedad(configuracion, PROPIEDAD_ALMACEN));
        assertEquals(LISTA_CRLS_OFICIAL, propiedad(configuracion, PROPIEDAD_LISTA_CRLS));
        // Explícita aunque AOP ya la tome como false si falta: así no depende del valor por defecto de la versión de AOP
        assertEquals("false", propiedad(configuracion, PROPIEDAD_DEMO_DATA));
    }

    @ParameterizedTest
    @ValueSource(strings = {"axelor-config.dev.properties", "axelor-config.pre.properties", "axelor-config.pro.properties"})
    void configuracionPrivada_laCriptografiaDeDemoSoloEnUnEntornoDeDemo(String fichero) throws Exception {
        Properties configuracion = configuracionPrivada(fichero);

        boolean almacenDemo = ALMACEN_DEMO.equals(propiedad(configuracion, PROPIEDAD_ALMACEN));
        boolean listaCrlsDemo = LISTA_CRLS_DEMO.equals(propiedad(configuracion, PROPIEDAD_LISTA_CRLS));
        String demoData = propiedad(configuracion, PROPIEDAD_DEMO_DATA);

        // Van juntos: con el almacén de demo y sin su CRL, la validación rechaza los certificados de demo
        assertEquals(almacenDemo, listaCrlsDemo, fichero + " debe usar a la vez, o ninguno, el almacén de demo y la lista de CRLs de demo");
        if (almacenDemo) {
            assertEquals("true", demoData, fichero + " usa la criptografía de demo sin cargar los datos de demo");
        }
    }

    @Test
    void configuracionPrivadaDeProduccion_usaLaCriptografiaOficialYNoCargaLaDemo() throws Exception {
        Properties configuracion = configuracionPrivada("axelor-config.pro.properties");

        assertEquals(ALMACEN_OFICIAL, propiedad(configuracion, PROPIEDAD_ALMACEN));
        assertEquals(LISTA_CRLS_OFICIAL, propiedad(configuracion, PROPIEDAD_LISTA_CRLS));
        assertEquals("false", propiedad(configuracion, PROPIEDAD_DEMO_DATA));
    }

    private static Properties configuracionPrivada(String fichero) throws Exception {
        Path ruta = DIR_CONFIGURACION_PRIVADA.resolve(fichero);
        assumeTrue(Files.isRegularFile(ruta), "No está la configuración privada: " + ruta.toAbsolutePath());

        Properties configuracion = new Properties();
        try (Reader reader = Files.newBufferedReader(ruta, StandardCharsets.UTF_8)) {
            configuracion.load(reader);
        }
        return configuracion;
    }

    /** Cada entorno MUST declarar la propiedad explícitamente, para no depender de lo que diga otro fichero. */
    private static String propiedad(Properties configuracion, String nombre) {
        String valor = configuracion.getProperty(nombre);
        assertNotNull(valor, "No se declara " + nombre);
        return valor.trim();
    }
}
