package com.educaflow.base.infrastructure.criptografia.impl.helper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.Provider;
import java.security.Security;
import java.security.UnrecoverableKeyException;
import java.security.cert.CRL;
import java.security.cert.CertStore;
import java.security.cert.CertificateFactory;
import java.security.cert.CollectionCertStoreParameters;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
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

    /**
     * Abre un almacén de claves en fichero, que siempre es un PKCS#12: el tipo no es un parámetro porque un
     * almacén PKCS#11 no se carga desde un InputStream, sino desde su Provider con la otra sobrecarga.
     */
    public static KeyStore getKeyStore(InputStream inputStreamKeyStore, String keyStorePassword) {
        try {
            KeyStore keyStore = KeyStore.getInstance(KeyStoreType.PKCS12.name());
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

    /**
     * Comprueba si el password abre el almacén de claves y permite recuperar sus claves privadas.
     *
     * <p>Solo vale para almacenes en fichero (PKCS#12): el PIN de un dispositivo PKCS#11 no se comprueba por
     * adelantado porque los intentos fallidos bloquean la tarjeta.
     *
     * @param inputStreamKeyStore contenido del almacén de claves
     * @param keyStorePassword password a comprobar
     * @return true si el password es correcto, false si no lo es
     * @throws RuntimeException si el almacén está corrupto o no es un PKCS#12
     */
    public static boolean isPasswordValid(InputStream inputStreamKeyStore, String keyStorePassword) {
        try {
            KeyStore keyStore = KeyStore.getInstance(KeyStoreType.PKCS12.name());
            keyStore.load(inputStreamKeyStore, keyStorePassword.toCharArray());

            // Abrir el almacén no basta: al firmar se recupera la clave privada, que puede tener su propio password.
            Enumeration<String> aliases = keyStore.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                if (keyStore.isKeyEntry(alias)) {
                    keyStore.getKey(alias, keyStorePassword.toCharArray());
                }
            }

            return true;
        } catch (IOException ex) {
            // El PKCS#12 envuelve en un IOException el fallo de MAC que provoca un password incorrecto.
            // Cualquier otra causa es un fichero corrupto o que no es un PKCS#12, no un password erróneo.
            if (ex.getCause() instanceof UnrecoverableKeyException) {
                return false;
            }
            throw new RuntimeException(ex);
        } catch (UnrecoverableKeyException ex) {
            // El password abre el almacén pero no recupera la clave privada.
            return false;
        } catch (GeneralSecurityException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Tipos de almacén de claves. Solo lo recibe la sobrecarga de {@code getKeyStore} que trabaja sobre un
     * {@code Provider}, que es la única en la que el tipo no está determinado por la forma de abrir el almacén.
     */
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
