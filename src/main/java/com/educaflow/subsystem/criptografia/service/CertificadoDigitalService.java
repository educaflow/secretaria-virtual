package com.educaflow.subsystem.criptografia.service;

import com.axelor.db.modelservice.ModelService;
import com.educaflow.base.infrastructure.criptografia.AlmacenClave;
import com.axelor.db.modelservice.BusinessMessages;
import com.educaflow.subsystem.criptografia.db.CertificadoDigital;

import java.util.Optional;

public interface CertificadoDigitalService extends ModelService<CertificadoDigital> {

    AlmacenClave getAlmacenClaveByDni(String dni);

    Optional<BusinessMessages> validateInsert(CertificadoDigital certificado);

    Optional<BusinessMessages> validateUpdate(CertificadoDigital certificado, CertificadoDigital certificadoOriginal);

    Optional<BusinessMessages> validateRemove(CertificadoDigital certificado);

}
