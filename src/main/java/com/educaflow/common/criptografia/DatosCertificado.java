package com.educaflow.common.criptografia;

import java.security.cert.X509Certificate;
import java.util.Date;

/**
 *
 * @author logongas
 */
public interface DatosCertificado {
    
    String getDNI();
    String getNombre();
    String getApellidos();
    String getCnSubject();
    String getCnIssuer();
    TipoEmisorCertificado getTipoEmisorCertificado();
    TipoCertificado getTipoCertificado();
    boolean isSelloTiempo();
    Date getValidoNoAntesDe();
    Date getValidoNoDespuesDe();
    X509Certificate getCertificate();
    String getCif();

    /**
     * Valida contra https://sedediatid.digital.gob.es/Prestadores/Paginas/Inicio.aspx
     * @return
     */
    boolean isValidoEnListaCertificadosConfiables();
    
}
