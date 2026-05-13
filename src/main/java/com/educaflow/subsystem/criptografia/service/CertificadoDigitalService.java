package com.educaflow.subsystem.criptografia.service;

import com.axelor.db.modelservice.ModelService;
import com.educaflow.base.infrastructure.criptografia.AlmacenClave;
import com.axelor.db.modelservice.BusinessMessages;
import com.educaflow.base.infrastructure.criptografia.AlmacenClaveFichero;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.criptografia.db.CertificadoDigital;

import java.util.Optional;

public interface CertificadoDigitalService extends ModelService<CertificadoDigital> {


    Optional<BusinessMessages> validateInsert(CertificadoDigital certificado);

    Optional<BusinessMessages> validateUpdate(CertificadoDigital certificado, CertificadoDigital certificadoOriginal);

    Optional<BusinessMessages> validateRemove(CertificadoDigital certificado);

    AlmacenClave getAlmacenClaveByDni(String dni);
}
