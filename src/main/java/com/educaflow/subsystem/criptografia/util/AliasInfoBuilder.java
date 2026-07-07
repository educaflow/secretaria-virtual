package com.educaflow.subsystem.criptografia.util;

import com.educaflow.base.infrastructure.criptografia.DatosCertificado;
import com.educaflow.base.infrastructure.criptografia.EntornoCriptografico;

import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;

/**
 * Construye el texto informativo (no persistido) de un {@code Alias}: los datos de negocio del
 * certificado asociado a ese alias en el dispositivo (interpretados por {@link DatosCertificado}).
 * Se lee <strong>en vivo</strong> del dispositivo mediante
 * {@link DispositivoCriptograficoInfoBuilder#readCertificadosPorAlias}.
 *
 * <p>Lo invoca el getter calculado del campo {@code info} del dominio, por lo que
 * <strong>nunca lanza excepción</strong>: ante cualquier error devuelve un mensaje que lo
 * describe, para no impedir que se cargue el formulario.
 */
public final class AliasInfoBuilder {

    private AliasInfoBuilder() {
    }

    public static String buildAliasInfo(String pkcs11LibraryPath, Integer slot, String pin, String alias) {
        if (pkcs11LibraryPath == null || pkcs11LibraryPath.isBlank() || alias == null || alias.isBlank()) {
            return "";
        }
        int slotIndex = slot != null ? slot : 0;
        try {
            X509Certificate certificado = DispositivoCriptograficoInfoBuilder.readCertificadosPorAlias(pkcs11LibraryPath, slotIndex, pin).get(alias);
            return formatCertificado(certificado);
        } catch (Exception ex) {
            return "No se pudo leer la información del alias: " + ex.getMessage();
        }
    }

    /**
     * Formatea, un dato debajo de otro, los datos de negocio del certificado de un alias
     * (los mismos que muestra la tabla «Firmas» de la verificación de PDF, salvo los propios de
     * una firma: fecha de firma, corrección, nombre de campo y motivo).
     */
    private static String formatCertificado(X509Certificate cert) {
        if (cert == null) {
            return "(el alias no tiene un certificado X.509 asociado)\n";
        }

        DatosCertificado datos;
        try {
            datos = EntornoCriptografico.getDatosCertificado(cert);
        } catch (Exception ex) {
            return "No se pudieron interpretar los datos del certificado: " + ex.getMessage() + "\n";
        }

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

        StringBuilder sb = new StringBuilder();
        appendCampo(sb, "Valido según TSL", datos.isValidoEnListaCertificadosConfiables() ? "sí" : "no");
        appendCampo(sb, "Es sello tiempo", datos.isSelloTiempo() ? "sí" : "no");
        appendCampo(sb, "CN Sujeto", datos.getCnSubject());
        appendCampo(sb, "Nombre", datos.getNombre());
        appendCampo(sb, "Apellidos", datos.getApellidos());
        appendCampo(sb, "DNI", datos.getDNI());
        appendCampo(sb, "CIF", datos.getCif());
        appendCampo(sb, "CN Emisor", datos.getCnIssuer());
        appendCampo(sb, "Tipo emisor certificado", datos.getTipoEmisorCertificado() != null ? datos.getTipoEmisorCertificado().toString() : "(desconocido)");
        appendCampo(sb, "Tipo Certificado", datos.getTipoCertificado() != null ? datos.getTipoCertificado().toString() : "(desconocido)");
        appendCampo(sb, "Fecha inicio", simpleDateFormat.format(datos.getValidoNoAntesDe()));
        appendCampo(sb, "Fecha fin", simpleDateFormat.format(datos.getValidoNoDespuesDe()));
        return sb.toString();
    }

    private static void appendCampo(StringBuilder sb, String etiqueta, String valor) {
        // Ancho fijo alineado a la etiqueta más larga ("Tipo emisor certificado").
        sb.append(String.format("%-24s %s%n", etiqueta + ":", valor != null ? valor : ""));
    }
}