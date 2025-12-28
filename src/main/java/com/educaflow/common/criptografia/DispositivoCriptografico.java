package com.educaflow.common.criptografia;

import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.*;

public class DispositivoCriptografico {

    private final List<String> aliases;
    private final Map<String,PrivateKey> privateKeys;
    private final Map<String,Certificate[]>  certificateChains;

    public DispositivoCriptografico(KeyStore devicePkcs11KeyStore,char[] pin) {
        aliases=getAliases(devicePkcs11KeyStore);
        privateKeys=getPrivateKeys(devicePkcs11KeyStore,pin,aliases);
        certificateChains=getCertificateChains(devicePkcs11KeyStore,aliases);
    }

    public PrivateKey getPrivateKey(String alias) {
        if (privateKeys.containsKey(alias)==false) {
            throw new RuntimeException("No existe la clave privada para el alias: " + alias);
        }
        return privateKeys.get(alias);
    }

    public Certificate[] getCertificateChain(String alias) {
        if (privateKeys.containsKey(alias)==false) {
            throw new RuntimeException("No existe el CertificateChain para el alias: " + alias);
        }
        return certificateChains.get(alias);
    }

    /**
     * Obtiene los alias de los certificados que hay en el dispositivo criptográfico
     * En el DNI hay: "CertAutenticacion" "CertFirmaDigital"
     * @return
     */
    public List<String> getAliases() {
        return aliases;
    }

    /**************************************************************/
    /********************* Funciones Privadas *********************/
    /**************************************************************/

    public List<String> getAliases(KeyStore keyStore) {
        try {
            List<String> aliases = new ArrayList<>();
            Enumeration<String> enumeration = keyStore.aliases();
            while (enumeration.hasMoreElements()) {
                aliases.add(enumeration.nextElement());
            }
            return aliases;
        } catch (Exception ex) {
            throw new RuntimeException("Error al obtener los alias del dispositivo criptográfico", ex);
        }
    }

    private Map<String, PrivateKey> getPrivateKeys(KeyStore keyStore, char[] pin,List<String> aliases) {
        try {
            Map<String, PrivateKey> privateKeys = new HashMap<>();

            for(String alias : aliases) {
                PrivateKey privateKey = (PrivateKey) keyStore.getKey(alias, pin);
                privateKeys.put(alias, privateKey);
            }

            return privateKeys;
        } catch (Exception ex) {
            throw new RuntimeException("Error al obtener las claves privadas del dispositivo criptográfico", ex);
        }
    }

    private Map<String, Certificate[]> getCertificateChains(KeyStore keyStore,List<String> aliases) {
        try {
            Map<String, Certificate[]> certificateChains = new HashMap<>();

            for(String alias : aliases) {
                Certificate[] certificateChain = keyStore.getCertificateChain(alias);
                certificateChains.put(alias, certificateChain);
            }

            return certificateChains;
        } catch (Exception ex) {
            throw new RuntimeException("Error al obtener el Certificate Chain del dispositivo criptográfico", ex);
        }
    }


}
