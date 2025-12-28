package com.educaflow.common.criptografia.impl.helper;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Enumeration;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.Extensions;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;

public class CertificateViewer {

    public static void print(Certificate certificate) throws Exception {
        // Carga tu certificado desde un archivo
        // Asegúrate de reemplazar "ruta/a/tu/certificado.cer" con la ruta real de tu archivo
        X509Certificate cert = (X509Certificate)certificate;

        System.out.println("--- Información General del Certificado ---");
        System.out.println("Sujeto: " + cert.getSubjectX500Principal());
        System.out.println("Emisor: " + cert.getIssuerX500Principal());
        System.out.println("Número de Serie: " + cert.getSerialNumber());
        System.out.println("Algoritmo de Firma: " + cert.getSigAlgName());

        System.out.println("\n--- Extensiones del Certificado ---");
        X509CertificateHolder holder = new JcaX509CertificateHolder(cert);
        Extensions extensions = holder.getExtensions();

        // Verificar si hay extensiones
        if (extensions != null) {
            Enumeration<ASN1ObjectIdentifier> oids = extensions.oids();
            while (oids.hasMoreElements()) {
                ASN1ObjectIdentifier oid = oids.nextElement();
                Extension ext = extensions.getExtension(oid);
                
                System.out.println("  OID: " + ext.getExtnId());
                System.out.println("  Nombre: " + getExtensionName(ext.getExtnId().getId()));
                System.out.println("  Crítica: " + ext.isCritical());

                try {
                    ASN1Primitive primitive = ASN1Primitive.fromByteArray(ext.getExtnValue().getOctets());
                    printAsn1Structure(primitive, "    ");
                } catch (Exception e) {
                    System.out.println("  Valor (base64): " + Base64.getEncoder().encodeToString(ext.getExtnValue().getOctets()));
                    System.out.println("  Error al parsear el valor: " + e.getMessage());
                }
                System.out.println("-------------------------------------");
            }
        }
    }

    private static String getExtensionName(String oid) {
        String extensionName;

        switch (oid) {
            case "2.5.29.17" :
                extensionName="Subject Alternative Name (SAN)";
                break;
            case "2.5.29.14" :
                extensionName="Subject Key Identifier";
                break;
            case "2.5.29.15" :
                extensionName="Key Usage";
                break;
            case "2.5.29.19" :
                extensionName="Basic Constraints";
                break;
            case "2.5.29.37" :
                extensionName="Extended Key Usage";
                break;
            case "1.3.6.1.4.1.5734.1.4" :
                extensionName="DNI (OID de ejemplo)";
                break;
            default :
                extensionName="OID Desconocido";
                break;
        };

        return extensionName;
    }

    private static void printAsn1Structure(ASN1Encodable obj, String indent) {
        if (obj == null) {
            System.out.println(indent + "null");
            return;
        }

        System.out.println(indent + obj.getClass().getSimpleName() + ": ");

        if (obj instanceof org.bouncycastle.asn1.ASN1Sequence) {
            for (ASN1Encodable item : (org.bouncycastle.asn1.ASN1Sequence) obj) {
                printAsn1Structure(item, indent + "  ");
            }
        } else if (obj instanceof org.bouncycastle.asn1.ASN1Set) {
            for (ASN1Encodable item : (org.bouncycastle.asn1.ASN1Set) obj) {
                printAsn1Structure(item, indent + "  ");
            }
        } else if (obj instanceof org.bouncycastle.asn1.ASN1TaggedObject) {
            org.bouncycastle.asn1.ASN1TaggedObject tagged = (org.bouncycastle.asn1.ASN1TaggedObject) obj;
            System.out.println(indent + "  Etiqueta: " + tagged.getTagNo());
            printAsn1Structure(tagged.getBaseObject(), indent + "  ");
        } else {
            System.out.println(indent + "  Valor: " + obj.toString());
        }
    }
}