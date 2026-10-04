package com.educaflow.subsystem.criptografia.util;

import com.axelor.i18n.I18n;
import com.educaflow.subsystem.criptografia.service.SituacionFirma;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CertificadoDigitalHelper {

    private CertificadoDigitalHelper() {
    }


    public static String motivoClaveErronea(SituacionFirma situacionFirma) {
        Objects.requireNonNull(situacionFirma, "situacionFirma no puede ser null");

        return switch (situacionFirma) {
            case DISPOSITIVO_SIN_PIN -> I18n.get("el PIN indicado no es correcto");
            case FICHERO_SIN_CLAVE -> I18n.get("la contraseña indicada no es correcta");
            case DISPOSITIVO_CON_PIN, FICHERO_CON_CLAVE -> I18n.get("la clave guardada de su certificado digital no es correcta. Póngase en contacto con el administrador");
            case SIN_DNI, SIN_CERTIFICADO ->I18n.get("ha fallado la firma en el servidor. Póngase en contacto con el administrador");
        };
    }



}
