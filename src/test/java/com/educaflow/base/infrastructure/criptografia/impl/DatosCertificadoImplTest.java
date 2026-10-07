package com.educaflow.base.infrastructure.criptografia.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

import com.educaflow.base.infrastructure.criptografia.TipoCertificado;
import com.educaflow.base.infrastructure.criptografia.TipoEmisorCertificado;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.DERUTF8String;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Caracteriza la extracción de los datos comunes (DNI, nombre, apellidos, CIF) según el emisor del
 * certificado de usuario final o de representación. Se usa un {@code trustedKeyStore} nulo para no
 * depender del almacén de certificados confiables.
 */
class DatosCertificadoImplTest {

    private static KeyPair keyPair;

    @BeforeAll
    static void crearClaves() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        keyPair = generator.generateKeyPair();
    }

    @Test
    void usuarioFinalFnmt_extraeDniNombreYApellidosDelSubjectAltName() throws Exception {
        X500Name issuer = issuer("FNMT-RCM", "AC FNMT Usuarios");
        X500Name dirName = new X500NameBuilder(BCStyle.INSTANCE)
                .addRDN(new ASN1ObjectIdentifier("1.3.6.1.4.1.5734.1.4"), new DERUTF8String("93882914L"))
                .addRDN(new ASN1ObjectIdentifier("1.3.6.1.4.1.5734.1.1"), new DERUTF8String("JUAN"))
                .addRDN(new ASN1ObjectIdentifier("1.3.6.1.4.1.5734.1.2"), new DERUTF8String("GARCIA"))
                .addRDN(new ASN1ObjectIdentifier("1.3.6.1.4.1.5734.1.3"), new DERUTF8String("LOPEZ"))
                .build();

        DatosCertificadoImpl datos = new DatosCertificadoImpl(certificado(sujeto("JUAN GARCIA"), issuer, dirName), null);

        assertEquals(TipoEmisorCertificado.FNMT, datos.getTipoEmisorCertificado());
        assertEquals(TipoCertificado.USUARIO_FINAL, datos.getTipoCertificado());
        assertEquals("93882914L", datos.getDNI());
        assertEquals("JUAN", datos.getNombre());
        assertEquals("GARCIA LOPEZ", datos.getApellidos());
        assertEquals("", datos.getCif());
    }

    @Test
    void usuarioFinalFnmt_sinDatosEnElCertificado_dejaNulosDniYNombreYApellidosVacios() throws Exception {
        X500Name issuer = issuer("FNMT-RCM", "AC FNMT Usuarios");

        DatosCertificadoImpl datos = new DatosCertificadoImpl(certificado(sujeto("JUAN GARCIA"), issuer, null), null);

        assertEquals(TipoEmisorCertificado.FNMT, datos.getTipoEmisorCertificado());
        assertEquals(null, datos.getDNI());
        assertEquals(null, datos.getNombre());
        assertEquals("", datos.getApellidos());
        assertEquals("", datos.getCif());
    }

    @Test
    void usuarioFinalFnmt_conUnSoloApellido_noAnyadeElSegundoApellidoNulo() throws Exception {
        X500Name issuer = issuer("FNMT-RCM", "AC FNMT Usuarios");
        X500Name dirName = new X500NameBuilder(BCStyle.INSTANCE)
                .addRDN(new ASN1ObjectIdentifier("1.3.6.1.4.1.5734.1.2"), new DERUTF8String("GARCIA"))
                .build();

        DatosCertificadoImpl datos = new DatosCertificadoImpl(certificado(sujeto("JUAN GARCIA"), issuer, dirName), null);

        assertEquals("GARCIA", datos.getApellidos());
    }

    @Test
    void representacionFnmt_extraeDniSinPrefijoIdcesYCifSinPrefijoVates() throws Exception {
        X500Name issuer = issuer("FNMT-RCM", "AC Representación");
        X500Name dirName = new X500NameBuilder(BCStyle.INSTANCE)
                .addRDN(new ASN1ObjectIdentifier("1.3.6.1.4.1.5734.1.4"), new DERUTF8String("IDCES-93882914L"))
                .addRDN(new ASN1ObjectIdentifier("1.3.6.1.4.1.5734.1.1"), new DERUTF8String("ANA"))
                .addRDN(new ASN1ObjectIdentifier("1.3.6.1.4.1.5734.1.2"), new DERUTF8String("PEREZ"))
                .addRDN(new ASN1ObjectIdentifier("1.3.6.1.4.1.5734.1.3"), new DERUTF8String("RUIZ"))
                .addRDN(new ASN1ObjectIdentifier("1.3.6.1.4.1.5734.1.7"), new DERUTF8String("VATES-Q9655676F"))
                .build();

        DatosCertificadoImpl datos = new DatosCertificadoImpl(certificado(sujeto("ANA PEREZ"), issuer, dirName), null);

        assertEquals(TipoEmisorCertificado.FNMT, datos.getTipoEmisorCertificado());
        assertEquals(TipoCertificado.REPRESENTACION, datos.getTipoCertificado());
        assertEquals("93882914L", datos.getDNI());
        assertEquals("ANA", datos.getNombre());
        assertEquals("PEREZ RUIZ", datos.getApellidos());
        assertEquals("Q9655676F", datos.getCif());
    }

    @Test
    void representacionFnmt_sinDatosEnElCertificado_dejaLosValoresPorDefectoSalvoNombreYApellidos() throws Exception {
        X500Name issuer = issuer("FNMT-RCM", "AC Representación");

        DatosCertificadoImpl datos = new DatosCertificadoImpl(certificado(sujeto("ANA PEREZ"), issuer, null), null);

        assertEquals(TipoCertificado.REPRESENTACION, datos.getTipoCertificado());
        // getDNIFromIDCES/getCIFFromVATES lanzan con null y se tragan la excepción: quedan "" (valor inicial)
        assertEquals("", datos.getDNI());
        assertEquals(null, datos.getNombre());
        assertEquals("", datos.getApellidos());
        assertEquals("", datos.getCif());
    }

    @Test
    void usuarioFinalAccv_extraeDniYNombreApellidosSeparadosPorBarra() throws Exception {
        X500Name issuer = issuer("ACCV", "ACCVCA-120");
        X500Name dirName = new X500NameBuilder(BCStyle.INSTANCE)
                .addRDN(BCStyle.UID, new DERUTF8String("93882914L"))
                .addRDN(BCStyle.CN, new DERUTF8String("JUAN|GARCIA|LOPEZ"))
                .build();

        DatosCertificadoImpl datos = new DatosCertificadoImpl(certificado(sujeto("JUAN GARCIA"), issuer, dirName), null);

        assertEquals(TipoEmisorCertificado.ACCV, datos.getTipoEmisorCertificado());
        assertEquals("93882914L", datos.getDNI());
        assertEquals("JUAN", datos.getNombre());
        assertEquals("GARCIA LOPEZ", datos.getApellidos());
        assertEquals("", datos.getCif());
    }

    @Test
    void usuarioFinalDnie_extraeLosDatosDelSubject() throws Exception {
        X500Name issuer = issuer("DIRECCION GENERAL DE LA POLICIA", "AC DNIE 004");
        X500Name subject = new X500NameBuilder(BCStyle.INSTANCE)
                .addRDN(BCStyle.C, "ES")
                .addRDN(BCStyle.SERIALNUMBER, "93882914L")
                .addRDN(BCStyle.GIVENNAME, "JUAN")
                .addRDN(BCStyle.SURNAME, "GARCIA")
                .addRDN(BCStyle.CN, "GARCIA LOPEZ, JUAN (AUTENTICACION)")
                .build();

        DatosCertificadoImpl datos = new DatosCertificadoImpl(certificado(subject, issuer, null), null);

        assertEquals(TipoEmisorCertificado.DNI, datos.getTipoEmisorCertificado());
        assertEquals("93882914L", datos.getDNI());
        assertEquals("JUAN", datos.getNombre());
        assertEquals("GARCIA LOPEZ", datos.getApellidos());
        assertEquals("", datos.getCif());
    }

    @Test
    void usuarioFinalDeEmisorDesconocido_dejaTodosLosDatosVacios() throws Exception {
        X500Name issuer = issuer("OTRA ENTIDAD", "OTRA CA");

        DatosCertificadoImpl datos = new DatosCertificadoImpl(certificado(sujeto("JUAN GARCIA"), issuer, null), null);

        assertEquals(null, datos.getTipoEmisorCertificado());
        assertEquals(TipoCertificado.USUARIO_FINAL, datos.getTipoCertificado());
        assertEquals("", datos.getDNI());
        assertEquals("", datos.getNombre());
        assertEquals("", datos.getApellidos());
        assertEquals("", datos.getCif());
    }

    @Test
    void emisorSinOrganizacion_loClasificaComoEmisorDesconocido() throws Exception {
        X500Name issuer = new X500NameBuilder(BCStyle.INSTANCE)
                .addRDN(BCStyle.C, "ES")
                .addRDN(BCStyle.CN, "OTRA CA")
                .build();

        DatosCertificadoImpl datos = new DatosCertificadoImpl(certificado(sujeto("JUAN GARCIA"), issuer, null), null);

        assertEquals(null, datos.getTipoEmisorCertificado());
        assertEquals(TipoCertificado.USUARIO_FINAL, datos.getTipoCertificado());
    }

    @Test
    void selloTiempo_sinExtendedKeyUsage_noEsSelloDeTiempo() throws Exception {
        DatosCertificadoImpl datos = new DatosCertificadoImpl(certificadoConEku(null, false), null);

        assertFalse(datos.isSelloTiempo());
    }

    @Test
    void selloTiempo_ekuSinTimeStamping_noEsSelloDeTiempo() throws Exception {
        DatosCertificadoImpl datos = new DatosCertificadoImpl(certificadoConEku(KeyPurposeId.id_kp_clientAuth, true), null);

        assertFalse(datos.isSelloTiempo());
    }

    @Test
    void selloTiempo_ekuTimeStampingNoCritico_noEsSelloDeTiempo() throws Exception {
        // sin ninguna extensión crítica getCriticalExtensionOIDs() devuelve null
        DatosCertificadoImpl datos = new DatosCertificadoImpl(certificadoConEku(KeyPurposeId.id_kp_timeStamping, false), null);

        assertFalse(datos.isSelloTiempo());
    }

    @Test
    void selloTiempo_ekuTimeStampingNoCriticoConOtraExtensionCritica_noEsSelloDeTiempo() throws Exception {
        X509v3CertificateBuilder builder = builder(sujeto("TSA"), issuer("OTRA ENTIDAD", "OTRA CA"));
        builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));
        builder.addExtension(Extension.extendedKeyUsage, false, new ExtendedKeyUsage(KeyPurposeId.id_kp_timeStamping));

        DatosCertificadoImpl datos = new DatosCertificadoImpl(firmar(builder), null);

        assertFalse(datos.isSelloTiempo());
    }

    @Test
    void selloTiempo_ekuTimeStampingCritico_esSelloDeTiempo() throws Exception {
        DatosCertificadoImpl datos = new DatosCertificadoImpl(certificadoConEku(KeyPurposeId.id_kp_timeStamping, true), null);

        assertTrue(datos.isSelloTiempo());
        // al ser sello de tiempo no se extraen los datos comunes
        assertEquals("", datos.getDNI());
        assertEquals("", datos.getNombre());
    }

    @Test
    void selloTiempo_errorAlLeerElEku_lanzaRuntimeException() throws Exception {
        X509Certificate certificado = spy(certificadoConEku(KeyPurposeId.id_kp_timeStamping, true));
        doThrow(new CertificateParsingException("roto")).when(certificado).getExtendedKeyUsage();

        RuntimeException ex = assertThrows(RuntimeException.class, () -> new DatosCertificadoImpl(certificado, null));

        // el constructor vuelve a envolver la RuntimeException del método
        RuntimeException interna = assertInstanceOf(RuntimeException.class, ex.getCause());
        assertEquals("Error analizando EKU del certificado", interna.getMessage());
        assertInstanceOf(CertificateParsingException.class, interna.getCause());
    }

    private static X509Certificate certificadoConEku(KeyPurposeId keyPurposeId, boolean critico) throws Exception {
        X509v3CertificateBuilder builder = builder(sujeto("TSA"), issuer("OTRA ENTIDAD", "OTRA CA"));
        builder.addExtension(Extension.basicConstraints, false, new BasicConstraints(false));
        if (keyPurposeId != null) {
            builder.addExtension(Extension.extendedKeyUsage, critico, new ExtendedKeyUsage(keyPurposeId));
        }
        return firmar(builder);
    }

    private static X509v3CertificateBuilder builder(X500Name subject, X500Name issuer) {
        return new JcaX509v3CertificateBuilder(
                issuer, BigInteger.ONE, Date.from(Instant.now().minusSeconds(1)),
                Date.from(Instant.now().plus(Duration.ofDays(1))), subject, keyPair.getPublic());
    }

    private static X509Certificate firmar(X509v3CertificateBuilder builder) throws Exception {
        return new JcaX509CertificateConverter().getCertificate(
                builder.build(new JcaContentSignerBuilder("SHA256withRSA").build(keyPair.getPrivate())));
    }

    private static X500Name issuer(String organizacion, String cn) {
        return new X500NameBuilder(BCStyle.INSTANCE)
                .addRDN(BCStyle.C, "ES")
                .addRDN(BCStyle.O, organizacion)
                .addRDN(BCStyle.CN, cn)
                .build();
    }

    private static X500Name sujeto(String cn) {
        return new X500NameBuilder(BCStyle.INSTANCE)
                .addRDN(BCStyle.C, "ES")
                .addRDN(BCStyle.CN, cn)
                .build();
    }

    private static X509Certificate certificado(X500Name subject, X500Name issuer, X500Name subjectAltDirName) throws Exception {
        X509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                issuer, BigInteger.ONE, Date.from(Instant.now().minusSeconds(1)),
                Date.from(Instant.now().plus(Duration.ofDays(1))), subject, keyPair.getPublic());
        builder.addExtension(Extension.basicConstraints, false, new BasicConstraints(false));
        if (subjectAltDirName != null) {
            builder.addExtension(Extension.subjectAlternativeName, false,
                    new GeneralNames(new GeneralName(GeneralName.directoryName, subjectAltDirName)));
        }
        return new JcaX509CertificateConverter().getCertificate(
                builder.build(new JcaContentSignerBuilder("SHA256withRSA").build(keyPair.getPrivate())));
    }
}
