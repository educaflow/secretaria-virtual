package com.educaflow.base.util;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberType;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;

public class NumeroTelefono {

    private static final String REGION_POR_DEFECTO = "ES";

    private static final int PREFIJO_ESPANA = 34;

    private static final PhoneNumberUtil PHONE_NUMBER_UTIL = PhoneNumberUtil.getInstance();

    private final PhoneNumber numero;

    public NumeroTelefono(String telefono) {
        PhoneNumber parseado;
        try {
            // La librería rechaza null y blancos con NumberParseException, no con NPE.
            parseado = PHONE_NUMBER_UTIL.parse(telefono, REGION_POR_DEFECTO);
        } catch (NumberParseException e) {
            parseado = null;
        }
        this.numero = parseado;
    }

    private boolean esValido() {
        return numero != null && PHONE_NUMBER_UTIL.isValidNumber(numero);
    }

    private boolean esMovil() {
        if (numero == null) {
            return false;
        }
        PhoneNumberType tipo = PHONE_NUMBER_UTIL.getNumberType(numero);
        return tipo == PhoneNumberType.MOBILE || tipo == PhoneNumberType.FIXED_LINE_OR_MOBILE ;
    }

    private boolean esDeEspana() {
        return numero != null && numero.getCountryCode() == PREFIJO_ESPANA;
    }

    public boolean esValidoDeEspana() {
        return esValido() && esDeEspana();
    }

    public boolean esMovilDeEspana() {
        return esValido() && esMovil() && esDeEspana();
    }

    public String enFormatoE164() {
        if (!esMovilDeEspana()) {
            throw new IllegalStateException("El teléfono no es un móvil de España válido");
        }
        return PHONE_NUMBER_UTIL.format(numero, PhoneNumberFormat.E164);
    }
}
