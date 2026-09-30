package com.educaflow.base.infrastructure.pdf.impl;

import com.educaflow.base.infrastructure.criptografia.DatosCertificado;
import com.educaflow.base.infrastructure.criptografia.EntornoCriptografico;
import com.itextpdf.signatures.PdfPKCS7;
import com.itextpdf.signatures.PdfSignature;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.GregorianCalendar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ResultadoFirmaImplTest {

    private static final DatosCertificado DATOS_A = mock(DatosCertificado.class);
    private static final DatosCertificado DATOS_B = mock(DatosCertificado.class);
    private static final Calendar FECHA_A = new GregorianCalendar(2024, Calendar.MARCH, 5, 10, 20, 30);
    private static final Calendar FECHA_B = new GregorianCalendar(2025, Calendar.JUNE, 1, 8, 0, 0);

    private ResultadoFirmaImpl crear(String nombreCampo, boolean correcta, Calendar fecha, DatosCertificado datos, String motivo) throws Exception {
        PdfPKCS7 pdfPKCS7 = mock(PdfPKCS7.class);
        PdfSignature pdfSignature = mock(PdfSignature.class);
        when(pdfPKCS7.getSigningCertificate()).thenReturn(mock(X509Certificate.class));
        when(pdfPKCS7.getSignDate()).thenReturn(fecha);
        when(pdfPKCS7.verifySignatureIntegrityAndAuthenticity()).thenReturn(correcta);
        when(pdfSignature.getReason()).thenReturn(motivo);

        try (MockedStatic<EntornoCriptografico> entorno = Mockito.mockStatic(EntornoCriptografico.class)) {
            entorno.when(() -> EntornoCriptografico.getDatosCertificado(any())).thenReturn(datos);
            return new ResultadoFirmaImpl(nombreCampo, pdfPKCS7, pdfSignature);
        }
    }

    private ResultadoFirmaImpl base() throws Exception {
        return crear("campo", true, FECHA_A, DATOS_A, "motivo");
    }

    @Test
    void equals_mismaInstancia_true() throws Exception {
        ResultadoFirmaImpl resultado = base();
        assertTrue(resultado.equals(resultado));
    }

    @Test
    void equals_null_false() throws Exception {
        assertFalse(base().equals(null));
    }

    @Test
    void equals_otroTipo_false() throws Exception {
        assertFalse(base().equals("campo"));
    }

    @Test
    void equals_mismosValores_trueYMismoHashCode() throws Exception {
        ResultadoFirmaImpl a = base();
        ResultadoFirmaImpl b = base();
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void equals_distintaCorrecta_false() throws Exception {
        assertNotEquals(base(), crear("campo", false, FECHA_A, DATOS_A, "motivo"));
    }

    @Test
    void equals_distintaFecha_false() throws Exception {
        assertNotEquals(base(), crear("campo", true, FECHA_B, DATOS_A, "motivo"));
    }

    @Test
    void equals_distintosDatosCertificado_false() throws Exception {
        assertNotEquals(base(), crear("campo", true, FECHA_A, DATOS_B, "motivo"));
    }

    @Test
    void equals_distintoNombreCampo_false() throws Exception {
        assertNotEquals(base(), crear("otro", true, FECHA_A, DATOS_A, "motivo"));
    }

    @Test
    void equals_distintoMotivo_false() throws Exception {
        assertNotEquals(base(), crear("campo", true, FECHA_A, DATOS_A, "otro motivo"));
    }

    @Test
    void equals_motivoNullEnAmbos_true() throws Exception {
        assertEquals(crear("campo", true, FECHA_A, DATOS_A, null), crear("campo", true, FECHA_A, DATOS_A, null));
    }

    @Test
    void constructor_copiaLosDatosDeLaFirma() throws Exception {
        ResultadoFirmaImpl resultado = crear("campo", true, FECHA_A, DATOS_A, "motivo");
        assertEquals("campo", resultado.getNombreCampo());
        assertTrue(resultado.isCorrecta());
        assertEquals(java.time.LocalDateTime.of(2024, 3, 5, 10, 20, 30), resultado.getFechaFirma());
        assertEquals(DATOS_A, resultado.getDatosCertificado());
        assertEquals("motivo", resultado.getMotivo());
    }
}
