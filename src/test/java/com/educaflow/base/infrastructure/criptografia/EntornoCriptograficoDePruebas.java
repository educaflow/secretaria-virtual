package com.educaflow.base.infrastructure.criptografia;

import com.educaflow.base.infrastructure.criptografia.config.AlmacenCertificadosConfiablesConfig;

import java.io.InputStream;
import java.util.List;

/**
 * El entorno criptográfico solo se puede configurar una vez por JVM y todos los tests comparten la misma:
 * los que firman o validan firmas lo configuran por aquí, que lo hace solo la primera vez.
 *
 * <p>Se configura con el almacén oficial y con la CRL de la CA de demo: así un certificado de la CA de demo valida
 * contra el almacén de demo que el test pase a {@code DatosCertificadoImpl} (sin una CRL vigente de su emisor el
 * {@code PKIXRevocationChecker} lo rechaza), y cualquier otro certificado sigue sin ser de confianza.
 */
public class EntornoCriptograficoDePruebas {

    private static final String RUTA_ALMACEN = "firma/AlmacenCertificadosConfiables/truststore.jks";
    private static final String PASSWORD_ALMACEN = "s3cr3T";
    private static final String RUTA_CRL_CA_DEMO = "firma/demo/almacen/educaflow-demo-ca.crl";

    private static boolean configurado = false;

    public static synchronized void configurar() {
        if (configurado) {
            return;
        }

        EntornoCriptografico.configureAlmacenCertificadosConfiables(new AlmacenCertificadosConfiablesConfig(recurso(RUTA_ALMACEN), PASSWORD_ALMACEN, List.of(recurso(RUTA_CRL_CA_DEMO))));
        EntornoCriptografico.configureDispositivosCriptograficos(null);
        configurado = true;
    }

    private static InputStream recurso(String rutaClasspath) {
        InputStream inputStream = EntornoCriptograficoDePruebas.class.getClassLoader().getResourceAsStream(rutaClasspath);
        if (inputStream == null) {
            throw new IllegalStateException("No se encuentra en el classpath: " + rutaClasspath);
        }
        return inputStream;
    }

}
