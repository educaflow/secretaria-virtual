package com.educaflow.shared.certificados;

import com.educaflow.base.infrastructure.criptografia.AlmacenClave;
import com.educaflow.base.infrastructure.criptografia.AlmacenClaveDispositivo;
import com.educaflow.base.infrastructure.criptografia.AlmacenClaveFichero;
import com.educaflow.secretariavirtual.startup.EntornoCriptograficoConfigProvider;
import com.educaflow.shared.common.db.Centro;

import java.io.InputStream;

public class AlmacenClaveLoader {

    public AlmacenClave getDirector(Centro centro) {
        AlmacenClaveDispositivo almacenClave=new AlmacenClaveDispositivo( 0,"CertFirmaDigitalDirector");
        //AlmacenClave almacenClave=new AlmacenClaveFichero(AlmacenClaveLoader.class.getClassLoader().getResourceAsStream("/firma/instalar_certificado_criptografico/director.p12"),"nada");


        return almacenClave;
    }

    public AlmacenClave getSecretario(Centro centro) {
        AlmacenClaveDispositivo almacenClave=new AlmacenClaveDispositivo( 0,"CertFirmaDigitalSecretario");
        //AlmacenClave almacenClave=new AlmacenClaveFichero(AlmacenClaveLoader.class.getClassLoader().getResourceAsStream("/firma/instalar_certificado_criptografico/secretario.p12"),"nada");

        return almacenClave;
    }


    public AlmacenClave getByDNI(String dni) {
        if ("1234567Z".equals(dni)) {
            return new AlmacenClaveFichero(AlmacenClaveLoader.class.getClassLoader().getResourceAsStream("/firma/mi_certificado.p12"),"nadanada");
        } else {
            throw new RuntimeException("No existe certificado para el DNI: " + dni);
        }

    }

    public AlmacenClave getDummy() {
        return new AlmacenClaveFichero(AlmacenClaveLoader.class.getClassLoader().getResourceAsStream("/firma/mi_certificado.p12"),"nadanada");
    }

}
