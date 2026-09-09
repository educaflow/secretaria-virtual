package com.educaflow.tramites.util.firma;

import com.axelor.meta.CallMethod;
import com.educaflow.base.util.SecurityUtil;
import com.educaflow.subsystem.criptografia.service.SituacionFirma;
import com.educaflow.subsystem.criptografia.util.CertificadoDigitalHelper;

public class FirmaServidorController {

    @CallMethod
    public String getSituacionFirma() {
        return getSituacionFirmaUsuarioAutenticado().name();
    }

    @CallMethod
    public boolean isFirmaEnServidor() {
        return getSituacionFirmaUsuarioAutenticado().isFirmaEnServidor();
    }

    private SituacionFirma getSituacionFirmaUsuarioAutenticado() {
        String dniFirmante = SecurityUtil.getUser().getDni();
        return CertificadoDigitalHelper.getSituacionFirmaByDni(dniFirmante);
    }
}
