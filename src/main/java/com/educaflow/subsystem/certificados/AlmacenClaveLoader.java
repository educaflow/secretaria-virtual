package com.educaflow.subsystem.certificados;

import com.educaflow.base.infrastructure.criptografia.AlmacenClave;
import com.educaflow.base.infrastructure.criptografia.AlmacenClaveFichero;
import com.educaflow.subsystem.criptografia.service.CertificadoDigitalService;
import com.educaflow.subsystem.common.db.Centro;
import com.google.inject.Inject;

public class AlmacenClaveLoader {

    @Inject
    private CertificadoDigitalService certificadoDigitalService;

    public AlmacenClave getDirector(Centro centro) {
        //AlmacenClaveDispositivo almacenClave=new AlmacenClaveDispositivo( 0,"CertFirmaDigitalDirector");
        AlmacenClave almacenClave=new AlmacenClaveFichero(AlmacenClaveLoader.class.getClassLoader().getResourceAsStream("/firma/instalar_certificado_criptografico/director.p12"),"nada");


        return almacenClave;
    }

    public AlmacenClave getSecretario(Centro centro) {
        //AlmacenClaveDispositivo almacenClave=new AlmacenClaveDispositivo( 0,"CertFirmaDigitalSecretario");
        AlmacenClave almacenClave=new AlmacenClaveFichero(AlmacenClaveLoader.class.getClassLoader().getResourceAsStream("/firma/instalar_certificado_criptografico/secretario.p12"),"nada");

        return almacenClave;
    }


    public AlmacenClave getByDNI(String dni) {
        return certificadoDigitalService.getAlmacenClaveByDni(dni);
    }

    public AlmacenClave getDummy() {
        return new AlmacenClaveFichero(AlmacenClaveLoader.class.getClassLoader().getResourceAsStream("/firma/mi_certificado.p12"),"nadanada");
    }

}
