package com.educaflow.common.criptografia.config;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;

public class AlmacenCertificadosConfiablesConfig {
    private final InputStream inputStream;
    private final String password;
    private final List<InputStream> certificateRevocationListsInputStream;

    public AlmacenCertificadosConfiablesConfig(InputStream inputStream, String password,List<InputStream> certificateRevocationListsInputStream) {
        this.inputStream = inputStream;
        this.password = password;
        this.certificateRevocationListsInputStream=certificateRevocationListsInputStream;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public String getPassword() {
        return password;
    }

    public List<InputStream> getCertificateRevocationListsInputStream() {
        return certificateRevocationListsInputStream;
    }
}
