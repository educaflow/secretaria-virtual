package com.educaflow.shared.certificados;

import com.educaflow.base.infrastructure.criptografia.AlmacenClave;
import com.educaflow.base.infrastructure.criptografia.AlmacenClaveDispositivo;
import com.educaflow.shared.configuracioncentro.db.Centro;

public class AlmacenClaveLoader {

    public AlmacenClave getDirector(Centro centro) {
        AlmacenClaveDispositivo almacenClave=new AlmacenClaveDispositivo( 0,"CertFirmaDigitalDirector");

        return almacenClave;
    }

    public AlmacenClave getSecretario(Centro centro) {
        AlmacenClaveDispositivo almacenClave=new AlmacenClaveDispositivo( 0,"CertFirmaDigitalSecretario");

        return almacenClave;
    }


    public AlmacenClave getByDNI(String dni) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

}
