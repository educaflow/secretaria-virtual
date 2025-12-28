package com.educaflow.common.pdf.impl;

import com.educaflow.common.criptografia.EntornoCriptografico;
import com.educaflow.common.criptografia.DatosCertificado;
import com.educaflow.common.pdf.ResultadoFirma;
import com.itextpdf.signatures.PdfPKCS7;
import com.itextpdf.signatures.PdfSignature;

import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.util.Calendar;

public class ResultadoFirmaImpl implements ResultadoFirma {

    private final DatosCertificado datosCertificado;
    private LocalDateTime fechaFirma;
    private final boolean correcta;
    private final String nombreCampo;
    private String motivo;

    public ResultadoFirmaImpl(String nombreCampo, PdfPKCS7 pdfPKCS7, PdfSignature pdfSignature) {

        try {
            X509Certificate certificate = pdfPKCS7.getSigningCertificate();
            Calendar signDateCalendar=pdfPKCS7.getSignDate();

            this.datosCertificado = EntornoCriptografico.getDatosCertificado(certificate);
            this.fechaFirma=toLocalDateTime(signDateCalendar);
            this.nombreCampo=nombreCampo;
            this.correcta = pdfPKCS7.verifySignatureIntegrityAndAuthenticity();
            this.motivo = pdfSignature.getReason();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public String getMotivo() {
        return motivo;
    }

    @Override
    public boolean isCorrecta() {
        return correcta;
    }

    @Override
    public LocalDateTime getFechaFirma() {
        return fechaFirma;
    }

    @Override
    public DatosCertificado getDatosCertificado() {
        return datosCertificado;
    }

    public String getNombreCampo() {
        return nombreCampo;
    }

    /********************************************************************/
    /**************************** Utilidades ****************************/
    /********************************************************************/

    private LocalDateTime toLocalDateTime(Calendar calendar) {
        LocalDateTime localDateTime = LocalDateTime.of(
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH),
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                calendar.get(Calendar.SECOND),
                calendar.get(Calendar.MILLISECOND) * 1_000_000
        );

        return localDateTime;
    }

}
