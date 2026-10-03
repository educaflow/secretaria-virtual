package com.educaflow.base.infrastructure.pdf;

import com.educaflow.base.infrastructure.criptografia.DatosCertificado;

import java.time.LocalDateTime;

public interface ResultadoFirma {

    boolean isCorrecta();
    boolean isCubreDocumentoCompleto();
    LocalDateTime getFechaFirma();
    DatosCertificado getDatosCertificado();
    String getNombreCampo();
    String getMotivo();
}
