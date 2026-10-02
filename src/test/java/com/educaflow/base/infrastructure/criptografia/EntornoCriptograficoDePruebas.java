package com.educaflow.base.infrastructure.criptografia;

/**
 * El entorno criptográfico solo se puede configurar una vez por JVM y todos los tests comparten la misma:
 * los que firman o validan firmas lo configuran por aquí, que lo hace solo la primera vez.
 */
public class EntornoCriptograficoDePruebas {

    private static boolean configurado = false;

    public static synchronized void configurar() {
        if (configurado) {
            return;
        }

        EntornoCriptografico.configureAlmacenCertificadosConfiables(null);
        EntornoCriptografico.configureDispositivosCriptograficos(null);
        configurado = true;
    }

}
