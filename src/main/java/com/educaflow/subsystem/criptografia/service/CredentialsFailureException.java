package com.educaflow.subsystem.criptografia.service;

import com.educaflow.base.util.DniUtil;
import com.educaflow.subsystem.criptografia.util.CertificadoDigitalHelper;

/**
 * La clave —contraseña del fichero o PIN del dispositivo— no abre el certificado digital del firmante.
 */
public class CredentialsFailureException extends RuntimeException {

    private final String dni;


    public CredentialsFailureException(String dni, Throwable causa) {
        super("La clave no abre el certificado digital del firmante con dni=" + DniUtil.enmascarar(dni), causa);
        this.dni = dni;
    }

    public String getDni() {
        return dni;
    }
}
