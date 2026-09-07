package com.educaflow.subsystem.criptografia.service;

import com.educaflow.base.infrastructure.criptografia.AlmacenClave;
import com.educaflow.base.infrastructure.pdf.CampoFirma;
import com.educaflow.base.infrastructure.pdf.DocumentoPdf;
import com.educaflow.base.util.DniUtil;
import com.educaflow.subsystem.criptografia.util.CertificadoDigitalHelper;
import com.google.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.login.LoginException;
import java.security.UnrecoverableKeyException;

/**
 * Firma un PDF en el servidor con el certificado digital que la secretaría virtual custodia para un DNI.
 */
public class FirmaEnServidorService {

    private static final Logger log = LoggerFactory.getLogger(FirmaEnServidorService.class);

    @Inject
    private AlmacenClaveResolver almacenClaveResolver;

    public DocumentoPdf firmar(String dni, String clave, DocumentoPdf documentoOriginal, CampoFirma campoFirma) {
        if (documentoOriginal == null) {
            throw new IllegalStateException("No hay documento que firmar");
        }

        try {
            AlmacenClave almacenClave = almacenClaveResolver.getByDNI(dni, clave);
            if (almacenClave == null) {
                throw new IllegalStateException("El firmante con dni=" + DniUtil.enmascarar(dni) + " no tiene certificado digital con el que firmar en el servidor");
            }


            SituacionFirma situacionFirma= CertificadoDigitalHelper.getSituacionFirmaByDni(dni);
            if (situacionFirma.isFirmaEnServidor()==false) {
                throw new IllegalStateException("El firmante con dni=" + DniUtil.enmascarar(dni) + " no tiene certificado digital con el que firmar en el servidor");
            }
            if (situacionFirma.isNecesitaClaveOPin()) {
                if ((clave==null) || (clave.isBlank())) {
                    throw new IllegalStateException("El firmante con dni=" + DniUtil.enmascarar(dni) + " necesita una clave para firmar en el servidor");
                }
            }

            if (CertificadoDigitalHelper.isClaveCertificadoCorrecta(dni, clave)==false) {
                throw new CredentialsFailureException(dni, new RuntimeException("La clave del certificado es incorrecta:"+dni+","+clave));
            }

            return documentoOriginal.firmar(almacenClave, campoFirma);
        } catch (RuntimeException ex) {
            log.error("No se ha podido firmar en el servidor el documento del firmante con dni={}", DniUtil.enmascarar(dni), ex);

            if (inExceptionCausedByCredentialsFailure(ex)) {
                throw new CredentialsFailureException(dni, ex);
            }

            throw ex;
        }
    }


    private boolean inExceptionCausedByCredentialsFailure(Throwable ex) {
        int LIMITE_CAUSAS_A_RECORRER=20;
        Throwable causa = ex;

        for (int nivel = 0; causa != null && nivel < LIMITE_CAUSAS_A_RECORRER; nivel++) {
            if (causa instanceof UnrecoverableKeyException || causa instanceof LoginException) {
                return true;
            }
            causa = causa.getCause();
        }

        return false;
    }

}
