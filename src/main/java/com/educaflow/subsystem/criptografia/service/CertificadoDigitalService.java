package com.educaflow.subsystem.criptografia.service;

import com.axelor.db.modelservice.ModelService;
import com.educaflow.base.infrastructure.criptografia.AlmacenClave;
import com.axelor.db.modelservice.BusinessMessages;
import com.educaflow.subsystem.criptografia.db.CertificadoDigital;

import java.util.Optional;

public interface CertificadoDigitalService extends ModelService<CertificadoDigital> {

    Optional<AlmacenClave> getAlmacenClaveByDni(String dni);
    Optional<AlmacenClave> getAlmacenClaveByDni(String dni, String claveAcceso);
    SituacionFirma getSituacionFirmaByDni(String dni);
    boolean isClaveCertificadoCorrecta(String dni, String clave);
    DatosTitular getDatosTitularByDni(String dni);



    Optional<BusinessMessages> validateGetAlmacenClaveByDni(String dni);
    Optional<BusinessMessages> validateGetAlmacenClaveByDni(String dni, String claveAcceso);
    Optional<BusinessMessages> validateGetSituacionFirmaByDni(String dni);
    Optional<BusinessMessages> validateIsClaveCertificadoCorrecta(String dni, String clave);
    Optional<BusinessMessages> validateGetDatosTitularByDni(String dni);

}
