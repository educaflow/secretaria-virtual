package com.educaflow.subsystem.criptografia.service;

import com.axelor.db.modelservice.ModelServiceFactory;
import com.educaflow.base.infrastructure.criptografia.AlmacenClave;
import com.educaflow.base.infrastructure.criptografia.AlmacenClaveFichero;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.criptografia.db.CertificadoDigital;
import com.google.inject.Inject;

public class AlmacenClaveResolver {

    @Inject
    private ModelServiceFactory modelServiceFactory;

    public AlmacenClave getDirector(Centro centro) {
        //AlmacenClaveDispositivo almacenClave=new AlmacenClaveDispositivo( 0,"CertFirmaDigitalDirector");
        AlmacenClave almacenClave=new AlmacenClaveFichero(AlmacenClaveResolver.class.getClassLoader().getResourceAsStream("/firma/instalar_certificado_criptografico/director.p12"),"nada");


        return almacenClave;
    }

    public AlmacenClave getSecretario(Centro centro) {
        //AlmacenClaveDispositivo almacenClave=new AlmacenClaveDispositivo( 0,"CertFirmaDigitalSecretario");
        AlmacenClave almacenClave=new AlmacenClaveFichero(AlmacenClaveResolver.class.getClassLoader().getResourceAsStream("/firma/instalar_certificado_criptografico/secretario.p12"),"nada");

        return almacenClave;
    }


    public AlmacenClave getByDNI(String dni) {
        CertificadoDigitalService certificadoDigitalService = (CertificadoDigitalService) modelServiceFactory.resolve(CertificadoDigital.class);
        return certificadoDigitalService.getAlmacenClaveByDni(dni);
    }

    public AlmacenClave getDummy() {
        return new AlmacenClaveFichero(AlmacenClaveResolver.class.getClassLoader().getResourceAsStream("/firma/mi_certificado.p12"),"nadanada");
    }

}
