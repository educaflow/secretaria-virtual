package com.educaflow.common.criptografia.impl.helper;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.Provider;
import java.security.Security;
import java.security.cert.CRL;
import java.security.cert.CertStore;
import java.security.cert.CertificateFactory;
import java.security.cert.CollectionCertStoreParameters;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CriptografiaUtil {

    /**
     * https://docs.oracle.com/en/java/javase/11/security/pkcs11-reference-guide1.html#GUID-C4ABFACB-B2C9-4E71-A313-79F881488BB9
     * https://docs.oracle.com/javase/8/docs/technotes/guides/security/p11guide.html#Config
     *
     * @param operatingSystemModulePath
     * @param slot Realmente es el slotListIndex no es SlotId
     * @return
     */
    public static Provider getProviderPKCS11(Path operatingSystemModulePath, int slot) {
        try {
            String pkcs11Config = String.format("name=DispositivoCriptograficoEducaFlow"+slot+"\nlibrary=%s\nslotListIndex=%d\n", operatingSystemModulePath.toAbsolutePath().toString(), slot);
            File tempFileConf = File.createTempFile("DispositivoCriptograficoEducaFlow", ".cfg");
            tempFileConf.deleteOnExit(); // se borrará al salir del programa
            Files.writeString(tempFileConf.toPath(), pkcs11Config);

            Provider pkcs11Provider = Security.getProvider("SunPKCS11").configure(tempFileConf.getAbsolutePath());
            Security.addProvider(pkcs11Provider);

            return pkcs11Provider;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

    }

    public static KeyStore getKeyStore(InputStream inputStreamKeyStore, String keyStorePassword,KeyStoreType type) {
        try {
            KeyStore keyStore = KeyStore.getInstance(type.name());
            keyStore.load(inputStreamKeyStore, keyStorePassword.toCharArray());

            return keyStore;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static KeyStore getKeyStore(Provider provider, String keyStorePassword, KeyStoreType type) {
        try {
            KeyStore keyStore = KeyStore.getInstance(type.name(),provider);
            keyStore.load(null, keyStorePassword.toCharArray());

            return keyStore;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public enum KeyStoreType {
        PKCS12,
        PKCS11
    }

    public static List<CRL> getCertificateRevocationLists(List<InputStream> crlsInputStream) {
        try {
            List<CRL> certificateRevocationLists = new ArrayList<>();

            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            for (InputStream crl : crlsInputStream) {
                Collection<? extends CRL> crls = cf.generateCRLs(crl);
                certificateRevocationLists.addAll(crls);
            }

            return certificateRevocationLists;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }


    public static CertStore getCertStore(List<CRL> crls) {
        try {
            CertStore store = CertStore.getInstance("Collection", new CollectionCertStoreParameters(crls));
            return store;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

}
