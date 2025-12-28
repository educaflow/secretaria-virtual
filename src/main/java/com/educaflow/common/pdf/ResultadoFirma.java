package com.educaflow.common.pdf;

import com.educaflow.common.criptografia.DatosCertificado;

import java.time.LocalDateTime;

public interface ResultadoFirma {

    boolean isCorrecta();
    LocalDateTime getFechaFirma();
    DatosCertificado getDatosCertificado();
    String getNombreCampo();
    String getMotivo();
}
