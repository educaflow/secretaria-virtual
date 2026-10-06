package com.educaflow.base.infrastructure.criptografia.impl.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import org.bouncycastle.asn1.DERUTF8String;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CertificateParserTest {

    private static final String OID_CN = "2.5.4.3";
    private static final String OID_C = "2.5.4.6";
    private static final String OID_SERIAL = "2.5.4.5";
    private static final String OID_SERVER_AUTH = KeyPurposeId.id_kp_serverAuth.getId();
    private static final String OID_CLIENT_AUTH = KeyPurposeId.id_kp_clientAuth.getId();

    private static X509Certificate cert;

    @BeforeAll
    static void crearCertificado() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();

        X500Name subject = new X500NameBuilder(BCStyle.INSTANCE)
                .addRDN(BCStyle.C, "ES")
                .addRDN(BCStyle.CN, new DERUTF8String("Nombre Sujeto"))
                .build();
        X500Name dirName = new X500NameBuilder(BCStyle.INSTANCE)
                .addRDN(BCStyle.SERIALNUMBER, new DERUTF8String("IDCES-93882914L"))
                .build();

        X509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                subject, BigInteger.ONE, Date.from(Instant.now().minusSeconds(1)),
                Date.from(Instant.now().plus(Duration.ofDays(1))), subject, keyPair.getPublic());
        builder.addExtension(Extension.basicConstraints, false, new BasicConstraints(false));
        builder.addExtension(Extension.extendedKeyUsage, false,
                new ExtendedKeyUsage(new KeyPurposeId[]{KeyPurposeId.id_kp_clientAuth, KeyPurposeId.id_kp_serverAuth}));
        builder.addExtension(Extension.subjectAlternativeName, false,
                new GeneralNames(new GeneralName(GeneralName.directoryName, dirName)));

        cert = new JcaX509CertificateConverter().getCertificate(
                builder.build(new JcaContentSignerBuilder("SHA256withRSA").build(keyPair.getPrivate())));
    }

    @Test
    void findOidsWithLocation_certificadoNoX509_devuelveMapaVacio() {
        Map<String, String> result = CertificateParser.findOidsWithLocation(mock(Certificate.class), OID_CN);

        assertTrue(result.isEmpty());
    }

    @Test
    void findOidsWithLocation_oidEnSujetoConValorUtf8_actualmenteNoLoEncuentra() {
        // Comportamiento ACTUAL (bug latente): X500Name.toASN1Primitive() devuelve una secuencia
        // cuyos elementos son objetos RDN (ni ASN1Sequence, ni ASN1Set, ni ASN1TaggedObject),
        // así que la búsqueda recursiva nunca entra en el Subject.
        Map<String, String> result = CertificateParser.findOidsWithLocation(cert, OID_CN);

        assertTrue(result.isEmpty());
    }

    @Test
    void findOidsWithLocation_oidConValorNoUtf8_noLoDevuelve() {
        // C=ES se codifica como PrintableString, no como DERUTF8String
        Map<String, String> result = CertificateParser.findOidsWithLocation(cert, OID_C);

        assertTrue(result.isEmpty());
    }

    @Test
    void findOidsWithLocation_oidDentroDeObjetoEtiquetadoDeExtension_loDevuelveConElOidDeLaExtension() {
        Map<String, String> result = CertificateParser.findOidsWithLocation(cert, OID_SERIAL);

        assertEquals(Map.of(Extension.subjectAlternativeName.getId(), "IDCES-93882914L"), result);
    }

    @Test
    void findOidsWithLocation_oidAlFinalDeLaSecuencia_noLoDevuelve() {
        Map<String, String> result = CertificateParser.findOidsWithLocation(cert, OID_SERVER_AUTH);

        assertTrue(result.isEmpty());
    }

    @Test
    void findOidsWithLocation_oidSeguidoDeOtroOid_noLoDevuelve() {
        Map<String, String> result = CertificateParser.findOidsWithLocation(cert, OID_CLIENT_AUTH);

        assertTrue(result.isEmpty());
    }

    @Test
    void findOidsWithLocation_oidInexistente_devuelveMapaVacio() {
        Map<String, String> result = CertificateParser.findOidsWithLocation(cert, "1.2.3.4.5");

        assertTrue(result.isEmpty());
    }
}
