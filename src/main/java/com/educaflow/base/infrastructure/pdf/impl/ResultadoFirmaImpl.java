package com.educaflow.base.infrastructure.pdf.impl;

import com.educaflow.base.infrastructure.criptografia.EntornoCriptografico;
import com.educaflow.base.infrastructure.criptografia.DatosCertificado;
import com.educaflow.base.infrastructure.pdf.ResultadoFirma;
import com.itextpdf.signatures.PdfPKCS7;
import com.itextpdf.signatures.PdfSignature;

import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.Objects;

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
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        ResultadoFirmaImpl that = (ResultadoFirmaImpl) obj;

        return correcta == that.correcta
                && Objects.equals(fechaFirma, that.fechaFirma)
                && Objects.equals(datosCertificado, that.datosCertificado)
                && Objects.equals(nombreCampo, that.nombreCampo)
                && Objects.equals(motivo, that.motivo);
    }


    @Override
    public int hashCode() {
        return Objects.hash(correcta, fechaFirma, datosCertificado, nombreCampo, motivo);
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
