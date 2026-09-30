package com.educaflow.base.infrastructure.criptografia.impl.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.DERTaggedObject;
import org.bouncycastle.asn1.DERUTF8String;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CertificateViewerTest {

    private static String getExtensionName(String oid) throws Exception {
        Method method = CertificateViewer.class.getDeclaredMethod("getExtensionName", String.class);
        method.setAccessible(true);
        return (String) method.invoke(null, oid);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "2.5.29.17|Subject Alternative Name (SAN)",
        "2.5.29.14|Subject Key Identifier",
        "2.5.29.15|Key Usage",
        "2.5.29.19|Basic Constraints",
        "2.5.29.37|Extended Key Usage",
        "1.3.6.1.4.1.5734.1.4|DNI (OID de ejemplo)",
        "2.5.29.32|OID Desconocido",
        "''|OID Desconocido"
    })
    void getExtensionName_devuelveElNombreConocidoODesconocido(String oid, String esperado) throws Exception {
        assertEquals(esperado, getExtensionName(oid));
    }

    private static String printAsn1Structure(ASN1Encodable obj, String indent) throws Exception {
        Method method = CertificateViewer.class.getDeclaredMethod("printAsn1Structure", ASN1Encodable.class, String.class);
        method.setAccessible(true);
        PrintStream original = System.out;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        System.setOut(new PrintStream(buffer, true, StandardCharsets.UTF_8));
        try {
            method.invoke(null, obj, indent);
        } finally {
            System.setOut(original);
        }
        return buffer.toString(StandardCharsets.UTF_8).replace("\r\n", "\n");
    }

    @Test
    void printAsn1Structure_conNullImprimeNull() throws Exception {
        assertEquals(">null\n", printAsn1Structure(null, ">"));
    }

    @Test
    void printAsn1Structure_conPrimitivoImprimeSuValor() throws Exception {
        assertEquals(
                ">ASN1Integer: \n>  Valor: 5\n",
                printAsn1Structure(new ASN1Integer(5), ">"));
    }

    @Test
    void printAsn1Structure_conSecuenciaRecorreSusElementosConMasSangria() throws Exception {
        DERSequence sequence = new DERSequence(new ASN1Encodable[] {new ASN1Integer(1), new DERUTF8String("hola")});
        assertEquals(
                """
                >DERSequence:\s
                >  ASN1Integer:\s
                >    Valor: 1
                >  DERUTF8String:\s
                >    Valor: hola
                """,
                printAsn1Structure(sequence, ">"));
    }

    @Test
    void printAsn1Structure_conSecuenciaVaciaSoloImprimeLaCabecera() throws Exception {
        assertEquals(">DERSequence: \n", printAsn1Structure(new DERSequence(), ">"));
    }

    @Test
    void printAsn1Structure_conConjuntoRecorreSusElementosConMasSangria() throws Exception {
        DERSet set = new DERSet(new ASN1Integer(7));
        assertEquals(
                """
                >DERSet:\s
                >  ASN1Integer:\s
                >    Valor: 7
                """,
                printAsn1Structure(set, ">"));
    }

    @Test
    void printAsn1Structure_conObjetoEtiquetadoImprimeEtiquetaYSuContenido() throws Exception {
        DERTaggedObject tagged = new DERTaggedObject(true, 3, new ASN1Integer(9));
        assertEquals(
                """
                >DERTaggedObject:\s
                >  Etiqueta: 3
                >  ASN1Integer:\s
                >    Valor: 9
                """,
                printAsn1Structure(tagged, ">"));
    }
}
