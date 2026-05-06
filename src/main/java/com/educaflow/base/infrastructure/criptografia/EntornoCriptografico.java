package com.educaflow.base.infrastructure.criptografia;

import com.educaflow.base.infrastructure.criptografia.config.AlmacenCertificadosConfiablesConfig;
import com.educaflow.base.infrastructure.criptografia.config.DispositivoCriptograficoConfig;
import com.educaflow.base.infrastructure.criptografia.impl.DatosCertificadoImpl;
import com.educaflow.base.infrastructure.criptografia.impl.helper.CriptografiaUtil;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.Provider;
import java.security.Security;
import java.security.cert.CRL;
import java.security.cert.X509Certificate;
import java.util.*;


public class EntornoCriptografico {
    private static boolean almacenConfigurado = false;
    private static boolean dispositivosConfigurado = false;
    private static AlmacenCertificadosConfiables almacenCertificadosConfiables;
    private static Map<Integer, DispositivoCriptografico> dispositivosCriptograficos = new HashMap<>();
    private static List<Provider> proveedoresPkcs11Registrados = new ArrayList<>();


    public static void configureAlmacenCertificadosConfiables(AlmacenCertificadosConfiablesConfig almacenCertificadosConfiablesConfig) {
        if (almacenConfigurado) {
            throw new RuntimeException("El almacén de certificados confiables ya ha sido configurado");
        }

        Security.addProvider(new BouncyCastleProvider());

        if (almacenCertificadosConfiablesConfig == null) {
            almacenCertificadosConfiables = new AlmacenCertificadosConfiables();
        } else {
            almacenCertificadosConfiables = createAlmacenCertificadosConfiables(almacenCertificadosConfiablesConfig);
        }

        almacenConfigurado = true;
    }

    public static void configureDispositivosCriptograficos(List<DispositivoCriptograficoConfig> dispositivoCritograficoConfigs) {
        for (Provider provider : proveedoresPkcs11Registrados) {
            Security.removeProvider(provider.getName());
        }
        proveedoresPkcs11Registrados.clear();

        if (dispositivoCritograficoConfigs == null) {
            dispositivosCriptograficos = new HashMap<>();
        } else {
            dispositivosCriptograficos = createDispositivosCriptograficos(dispositivoCritograficoConfigs);
        }

        dispositivosConfigurado = true;
    }


    public static AlmacenCertificadosConfiables getAlmacenCertificadosConfiables() {
        if (!almacenConfigurado) {
            throw new RuntimeException("El almacén de certificados confiables no ha sido configurado");
        }
        return almacenCertificadosConfiables;
    }

    public static DispositivoCriptografico getDispositivoCriptografico(int slot) {
        if (!dispositivosConfigurado) {
            throw new RuntimeException("Los dispositivos criptográficos no han sido configurados");
        }
        if (!dispositivosCriptograficos.containsKey(slot)) {
            throw new RuntimeException("No se ha configurado ningún proveedor PKCS#11 para el slot: " + slot);
        }

        return dispositivosCriptograficos.get(slot);
    }


    public static DatosCertificado getDatosCertificado(X509Certificate certificate) {
        if (!almacenConfigurado) {
            throw new RuntimeException("El almacén de certificados confiables no ha sido configurado");
        }
        return new DatosCertificadoImpl(certificate, getAlmacenCertificadosConfiables().getTrustedKeyStore());
    }

    /*********************************************************************************/
    /***************************** Creación de los datos *****************************/
    /*********************************************************************************/

    private static AlmacenCertificadosConfiables createAlmacenCertificadosConfiables(AlmacenCertificadosConfiablesConfig almacenCertificadosConfiablesConfig) {
        try {
            InputStream inputStream = almacenCertificadosConfiablesConfig.getInputStream();
            String password = almacenCertificadosConfiablesConfig.getPassword();

            KeyStore trustedKeyStore = CriptografiaUtil.getKeyStore(inputStream, password, CriptografiaUtil.KeyStoreType.PKCS12);
            List<InputStream> certificateRevocationListsInputStream = almacenCertificadosConfiablesConfig.getCertificateRevocationListsInputStream();

            List<CRL> certificateRevocationLists;
            if (certificateRevocationListsInputStream == null) {
                certificateRevocationLists = new ArrayList<>();
            } else {
                certificateRevocationLists = CriptografiaUtil.getCertificateRevocationLists(certificateRevocationListsInputStream);
            }

            return new AlmacenCertificadosConfiables(trustedKeyStore, certificateRevocationLists);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }


    private static Map<Integer, DispositivoCriptografico> createDispositivosCriptograficos(List<DispositivoCriptograficoConfig> dispositivoCritograficoConfigs) {
        Map<Integer, DispositivoCriptografico> dispositivosCriptograficos = new HashMap<>();

        for (DispositivoCriptograficoConfig dispositivoCriptograficoConfig : dispositivoCritograficoConfigs) {
            int slot = dispositivoCriptograficoConfig.getSlot();

            if (dispositivosCriptograficos.containsKey(slot)) {
                throw new RuntimeException("Ya existe un PKCS#11 para el slot: " + slot);
            }

            DispositivoCriptografico dispositivoCriptografico = createDispositivoCriptografico(dispositivoCriptograficoConfig);
            dispositivosCriptograficos.put(slot, dispositivoCriptografico);
        }

        return dispositivosCriptograficos;
    }

    private static DispositivoCriptografico createDispositivoCriptografico(DispositivoCriptograficoConfig dispositivoCriptograficoConfig) {
        Path pkcs11LibraryPath = dispositivoCriptograficoConfig.getPkcs11LibraryPath();
        int slot = dispositivoCriptograficoConfig.getSlot();
        String pin = dispositivoCriptograficoConfig.getPin();

        if (!Files.exists(pkcs11LibraryPath)) {
            throw new RuntimeException("La librería PKCS#11 no existe: " + pkcs11LibraryPath);
        }
        if (!Files.isRegularFile(pkcs11LibraryPath)) {
            throw new RuntimeException("La librería PKCS#11 no es un archivo regular: " + pkcs11LibraryPath);
        }

        Provider providerPkcs11 = CriptografiaUtil.getProviderPKCS11(pkcs11LibraryPath, slot);
        proveedoresPkcs11Registrados.add(providerPkcs11);
        KeyStore devicePkcs11KeyStore = CriptografiaUtil.getKeyStore(providerPkcs11, pin, CriptografiaUtil.KeyStoreType.PKCS11);

        return new DispositivoCriptografico(devicePkcs11KeyStore, pin.toCharArray());
    }


}