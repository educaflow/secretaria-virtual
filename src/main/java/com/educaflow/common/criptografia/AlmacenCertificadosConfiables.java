package com.educaflow.common.criptografia;

import java.security.KeyStore;
import java.security.cert.CRL;
import java.util.ArrayList;
import java.util.List;

public class AlmacenCertificadosConfiables {

    final private KeyStore trustedKeyStore;
    final private List<CRL> certificateRevocationLists;

    public AlmacenCertificadosConfiables() {
        this.trustedKeyStore = null;
        this.certificateRevocationLists = new ArrayList<CRL>();
    }

    public AlmacenCertificadosConfiables(KeyStore trustedKeyStore,List<CRL> certificateRevocationLists) {
        this.trustedKeyStore = trustedKeyStore;
        if (certificateRevocationLists==null) {
            this.certificateRevocationLists = new ArrayList<CRL>();
        } else {
            this.certificateRevocationLists = certificateRevocationLists;
        }
    }

    public KeyStore getTrustedKeyStore() {
        return trustedKeyStore;
    }

    public List<CRL> getCertificateRevocationLists() {
        return certificateRevocationLists;
    }
}
