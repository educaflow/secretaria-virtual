package com.educaflow.base.infrastructure.pdf;

import com.educaflow.base.infrastructure.criptografia.DatosCertificado;

import java.time.LocalDateTime;

public interface ResultadoFirma {

    boolean isCorrecta();
    LocalDateTime getFechaFirma();
    DatosCertificado getDatosCertificado();
    String getNombreCampo();
    String getMotivo();
}
