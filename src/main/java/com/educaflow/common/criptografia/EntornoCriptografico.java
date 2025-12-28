package com.educaflow.common.criptografia;

import com.educaflow.common.criptografia.config.AlmacenCertificadosConfiablesConfig;
import com.educaflow.common.criptografia.config.EntornoCriptograficoConfig;
import com.educaflow.common.criptografia.config.DispositivoCriptograficoConfig;
import com.educaflow.common.criptografia.impl.DatosCertificadoImpl;
import com.educaflow.common.criptografia.impl.helper.CriptografiaUtil;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.Provider;
import java.security.Security;
import java.security.cert.CRL;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;


public class EntornoCriptografico {
    private static boolean configured = false;
    private static AlmacenCertificadosConfiables almacenCertificadosConfiables;
    private static Map<Integer, DispositivoCriptografico> dispositivosCriptograficos;



    public static void configure(EntornoCriptograficoConfig entornoCriptograficoConfig) {
        if (configured) {
            throw new RuntimeException("El entorno criptográfico ya ha sido configurado");
        }

        Security.addProvider(new BouncyCastleProvider());

        if (entornoCriptograficoConfig == null) {
            almacenCertificadosConfiables=new AlmacenCertificadosConfiables();
            dispositivosCriptograficos=new HashMap<>();
        } else {
            AlmacenCertificadosConfiablesConfig almacenCertificadosConfiablesConfig= entornoCriptograficoConfig.getAlmacenCertificadosConfiablesConfig();
            List<DispositivoCriptograficoConfig> dispositivoCritograficoConfigs= entornoCriptograficoConfig.getDispositivoCritograficoConfigs();


            if (almacenCertificadosConfiablesConfig == null) {
                almacenCertificadosConfiables=new AlmacenCertificadosConfiables();
            } else {
                almacenCertificadosConfiables=getAlmacenCertificadosConfiables(almacenCertificadosConfiablesConfig);
            }

            if (dispositivoCritograficoConfigs==null) {
                dispositivosCriptograficos=new HashMap<>();
            } else {
                dispositivosCriptograficos=getDispositivosCriptograficos(dispositivoCritograficoConfigs);
            }
        }






        configured=true;
    }


    public static AlmacenCertificadosConfiables getAlmacenCertificadosConfiables() {
        if (configured==false) {
            throw new RuntimeException("El entorno criptográfico no ha sido configurado");
        }
        return almacenCertificadosConfiables;
    }

    public static DispositivoCriptografico getDispositivoCriptografico(int slot) {
        if (configured==false) {
            throw new RuntimeException("El entorno criptográfico no ha sido configurado");
        }
        if (!dispositivosCriptograficos.containsKey(slot)) {
            throw new RuntimeException("No se ha configurado ningún proveedor PKCS#11 para el slot: " + slot);
        }

        return dispositivosCriptograficos.get(slot);
    }


    public static DatosCertificado getDatosCertificado(X509Certificate certificate) {
        if (configured == false) {
            throw new RuntimeException("El entorno criptográfico no ha sido configurado");
        }
        return new DatosCertificadoImpl(certificate,getAlmacenCertificadosConfiables().getTrustedKeyStore());
    }

    /*********************************************************************************/
    /***************************** Creación de los datos *****************************/
    /*********************************************************************************/

    private static AlmacenCertificadosConfiables getAlmacenCertificadosConfiables(AlmacenCertificadosConfiablesConfig almacenCertificadosConfiablesConfig) {
        try {
            InputStream inputStream=almacenCertificadosConfiablesConfig.getInputStream();
            String password=almacenCertificadosConfiablesConfig.getPassword();

            AlmacenCertificadosConfiables almacenCertificadosConfiables;

            KeyStore trustedKeyStore = CriptografiaUtil.getKeyStore(inputStream,password, CriptografiaUtil.KeyStoreType.PKCS12);
            List<InputStream> certificateRevocationListsInputStream=almacenCertificadosConfiablesConfig.getCertificateRevocationListsInputStream();

            List<CRL> certificateRevocationLists;
            if (certificateRevocationListsInputStream==null) {
                certificateRevocationLists=new ArrayList<>();
            } else {
                certificateRevocationLists = CriptografiaUtil.getCertificateRevocationLists(certificateRevocationListsInputStream);
            }

            almacenCertificadosConfiables = new AlmacenCertificadosConfiables(trustedKeyStore,certificateRevocationLists);

            return almacenCertificadosConfiables;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }


    private static Map<Integer, DispositivoCriptografico> getDispositivosCriptograficos(List<DispositivoCriptograficoConfig> dispositivoCritograficoConfigs) {
        Map<Integer, DispositivoCriptografico> dispositivosCriptograficos = new HashMap<>();

        for(DispositivoCriptograficoConfig dispositivoCriptograficoConfig : dispositivoCritograficoConfigs) {
            int slot = dispositivoCriptograficoConfig.getSlot();

            if (dispositivosCriptograficos.containsKey(slot)) {
                throw new RuntimeException("Yas existe un PKCS#11 para el slot: " + slot);
            }

            DispositivoCriptografico dispositivoCriptografico=getDispositivoCriptografico(dispositivoCriptograficoConfig);
            dispositivosCriptograficos.put(slot,dispositivoCriptografico);
        }

        return dispositivosCriptograficos;
    }

    private static DispositivoCriptografico getDispositivoCriptografico(DispositivoCriptograficoConfig dispositivoCriptograficoConfig) {
        Path pkcs11LibraryPath = dispositivoCriptograficoConfig.getPkcs11LibraryPath();
        int slot = dispositivoCriptograficoConfig.getSlot();
        String pin = dispositivoCriptograficoConfig.getPin();

        if (Files.exists(pkcs11LibraryPath)==false) {
            throw new RuntimeException("La librería PKCS#11 no existe" + pkcs11LibraryPath);
        }
        if (Files.isRegularFile(pkcs11LibraryPath)==false) {
            throw new RuntimeException("La librería PKCS#11 no es un archivo regular" + pkcs11LibraryPath);
        }

        Provider providerPkcs11 = CriptografiaUtil.getProviderPKCS11(pkcs11LibraryPath, slot);
        KeyStore devicePkcs11KeyStore=CriptografiaUtil.getKeyStore(providerPkcs11,pin, CriptografiaUtil.KeyStoreType.PKCS11);
        DispositivoCriptografico dispositivoCriptografico=new DispositivoCriptografico(devicePkcs11KeyStore, pin.toCharArray());

        return dispositivoCriptografico;

    }


}

