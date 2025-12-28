package com.educaflow.common.pdf.impl.helper;



import com.itextpdf.signatures.IExternalSignature;
import com.itextpdf.signatures.ISignatureMechanismParams;
import java.security.*;
import java.util.ArrayList;
import java.util.List;

public class PKCS11ExternalSignature implements IExternalSignature {

    private final PrivateKey privateKey;
    private final String digestAlgorithm;
    private final String encryptionAlgorithm;
    private final List<String> algoritmosDisponibles;

    public PKCS11ExternalSignature(PrivateKey privateKey,
                                   String digestAlgorithm,
                                   String encryptionAlgorithm) {
        this.privateKey = privateKey;
        this.digestAlgorithm = digestAlgorithm;
        this.encryptionAlgorithm = encryptionAlgorithm;
        this.algoritmosDisponibles=getAlgoritmosDisponibles();
    }

    @Override
    public byte[] sign(byte[] message) throws GeneralSecurityException {
        String algoritmo = getAlgoritmo(digestAlgorithm,encryptionAlgorithm);
        Signature signature = Signature.getInstance(algoritmo);
        signature.initSign(privateKey);
        signature.update(message);
        return signature.sign();
    }

    @Override
    public String getDigestAlgorithmName() {
        return digestAlgorithm;
    }

    @Override
    public String getSignatureAlgorithmName() {
        return encryptionAlgorithm;
    }

    @Override
    public ISignatureMechanismParams getSignatureMechanismParameters() {
        return null;
    }


    private String getAlgoritmo(String digestAlgorithm,String encryptionAlgorithm) {
        String algoritmo = digestAlgorithm.replace("-", "") + "WITH" + encryptionAlgorithm;

        if (algoritmosDisponibles.contains(algoritmo)==false) {
            throw new RuntimeException("El algoritmo '" + algoritmo + "' no está disponible en el sistema. Revisa el código de esta función para adecuarla al formato del String de los algoritmos disposibles:\n\n" + String.join(", ", algoritmosDisponibles));
        }

        return algoritmo;
    }



    private List<String> getAlgoritmosDisponibles() {
        List<String> algoritmos=new ArrayList<>();

        Provider[] providers= Security.getProviders("Signature.NONEwithRSA");

        for(Provider provider:providers) {
            for (Provider.Service service : provider.getServices()) {
                if (service.getType().equals("Signature")) {
                    algoritmos.add(service.getAlgorithm());
                }
            }
        }

        return algoritmos;
    }

}

