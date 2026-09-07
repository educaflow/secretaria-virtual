package com.educaflow.subsystem.criptografia.util;

import com.axelor.db.modelservice.ModelServiceFactory;
import com.axelor.i18n.I18n;
import com.axelor.inject.Beans;
import com.educaflow.base.infrastructure.criptografia.AlmacenClave;
import com.educaflow.base.infrastructure.criptografia.AlmacenClaveFichero;
import com.educaflow.subsystem.criptografia.db.CertificadoDigital;
import com.educaflow.subsystem.criptografia.service.SituacionFirma;
import com.educaflow.subsystem.criptografia.service.CertificadoDigitalService;
import com.educaflow.base.util.DniUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CertificadoDigitalHelper {

    private static final Logger log = LoggerFactory.getLogger(CertificadoDigitalHelper.class);

    private CertificadoDigitalHelper() {
    }


    public static SituacionFirma getSituacionFirmaByDni(String dni) {
        if (DniUtil.isValid(dni) == false) {
            return SituacionFirma.SIN_DNI;
        }

        try {
            CertificadoDigitalService certificadoDigitalService = getCertificadoDigitalService();
            SituacionFirma situacionFirma = certificadoDigitalService.getSituacionFirmaByDni(dni);

            return (situacionFirma == null) ? SituacionFirma.SIN_CERTIFICADO : situacionFirma;
        } catch (Exception ex) {
            log.error("No se pudo determinar la situación de firma del firmante con dni={}", DniUtil.enmascarar(dni), ex);
            return SituacionFirma.SIN_CERTIFICADO;
        }
    }

    public static boolean isClaveCertificadoCorrecta(String dni, String clave) {
        CertificadoDigitalService certificadoDigitalService = getCertificadoDigitalService();

        AlmacenClave almacenClave = certificadoDigitalService.getAlmacenClaveByDni(dni, clave);

        if (almacenClave==null) {
            return true;
        }

        if (almacenClave instanceof AlmacenClaveFichero almacenClaveFichero) {
            return almacenClaveFichero.isPasswordValid() == true;
        }

        return true;
    }




    public static String motivoClaveErronea(SituacionFirma situacionFirma) {
        if (situacionFirma == null) {
            throw new NullPointerException("situacionFirma es null");
        }

        return switch (situacionFirma) {
            case DISPOSITIVO_SIN_PIN -> I18n.get("el PIN indicado no es correcto");
            case FICHERO_SIN_CLAVE -> I18n.get("la contraseña indicada no es correcta");
            case DISPOSITIVO_CON_PIN, FICHERO_CON_CLAVE -> I18n.get("la clave guardada de su certificado digital no es correcta. Póngase en contacto con el administrador");
            case SIN_DNI, SIN_CERTIFICADO ->I18n.get("ha fallado la firma en el servidor. Póngase en contacto con el administrador");
        };
    }


    private static  CertificadoDigitalService getCertificadoDigitalService() {
        return (CertificadoDigitalService) Beans.get(ModelServiceFactory.class).resolve(CertificadoDigital.class);
    }

}
