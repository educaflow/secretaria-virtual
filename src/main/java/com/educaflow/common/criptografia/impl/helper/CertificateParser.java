package com.educaflow.common.criptografia.impl.helper;

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.DERUTF8String;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.Extensions;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;

public class CertificateParser {

    /**
     * Busca todos los valores de un OID en el certificado y los devuelve en un
     * mapa que asocia su ubicación con el valor encontrado.
     *
     * @param cert El certificado a analizar.
     * @param targetOid El OID del valor a buscar (p.ej., 2.5.4.3).
     * @return Un mapa donde la clave es la ubicación (p.ej., "Subject" o el OID
     * de la extensión) y el valor es el contenido encontrado. El mapa estará
     * vacío si no se encuentra ninguno.
     */
    public static Map<String, String> findOidsWithLocation(Certificate cert, String targetOid) {
        try {
            Map<String, String> results = new LinkedHashMap<>();
            if (!(cert instanceof X509Certificate)) {
                return results;
            }

            X509Certificate x509Cert = (X509Certificate) cert;
            X509CertificateHolder holder = new JcaX509CertificateHolder(x509Cert);

            // 1. Buscar en el Sujeto del certificado
            recursiveFindWithLocation(holder.getSubject().toASN1Primitive(), targetOid, "Subject", results);

            // 2. Buscar en las extensiones del certificado
            Extensions extensions = holder.getExtensions();
            if (extensions != null) {
                Enumeration<ASN1ObjectIdentifier> oids = extensions.oids();
                while (oids.hasMoreElements()) {
                    ASN1ObjectIdentifier oid = oids.nextElement();
                    Extension ext = extensions.getExtension(oid);
                    recursiveFindWithLocation(ASN1Primitive.fromByteArray(ext.getExtnValue().getOctets()), targetOid, oid.getId(), results);
                }
            }

            return results;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Función recursiva para buscar un OID y su valor, agregando todas las
     * coincidencias a un mapa con su ubicación.
     */
    private static void recursiveFindWithLocation(ASN1Encodable obj, String targetOid, String location, Map<String, String> results) {
        if (obj instanceof ASN1Sequence) {
            ASN1Sequence sequence = (ASN1Sequence) obj;
            for (int i = 0; i < sequence.size(); i++) {
                ASN1Encodable item = sequence.getObjectAt(i);

                if (item instanceof ASN1ObjectIdentifier) {
                    ASN1ObjectIdentifier oid = (ASN1ObjectIdentifier) item;
                    if (oid.getId().equals(targetOid) && (i + 1) < sequence.size()) {
                        ASN1Encodable nextItem = sequence.getObjectAt(i + 1);
                        if (nextItem instanceof DERUTF8String) {
                            results.put(location, ((DERUTF8String) nextItem).getString());
                        }
                    }
                } else {
                    recursiveFindWithLocation(item, targetOid, location, results);
                }
            }
        } else if (obj instanceof ASN1Set) {
            ASN1Set set = (ASN1Set) obj;
            for (ASN1Encodable item : set) {
                recursiveFindWithLocation(item, targetOid, location, results);
            }
        } else if (obj instanceof ASN1TaggedObject) {
            ASN1TaggedObject tagged = (ASN1TaggedObject) obj;
            recursiveFindWithLocation(tagged.getBaseObject(), targetOid, location, results);
        }
    }
    
    
    /**
     * Devuelve el DNI a partir de un valor con prefijo "IDCES-".
     * 
     * @param value Ejemplo: "IDCES-73544886V"
     * @return El DNI sin prefijo (ej. "73544886V")
     * @throws IllegalArgumentException si el valor no empieza por "IDCES-"
     */
    public static String getDNIFromIDCES(String value) {
        if (value == null || !value.startsWith("IDCES-")) {
            throw new IllegalArgumentException("Valor inválido para DNI con prefijo IDCES: " + value);
        }
        return value.substring("IDCES-".length());
    }

    /**
     * Devuelve el CIF/NIF a partir de un valor con prefijo "VATES-".
     * 
     * @param value Ejemplo: "VATES-Q9655676F"
     * @return El CIF/NIF sin prefijo (ej. "Q9655676F")
     * @throws IllegalArgumentException si el valor no empieza por "VATES-"
     */
    public static String getCIFFromVATES(String value) {
        if (value == null || !value.startsWith("VATES-")) {
            throw new IllegalArgumentException("Valor inválido para CIF con prefijo VATES: " + value);
        }
        return value.substring("VATES-".length());
    }    

}
