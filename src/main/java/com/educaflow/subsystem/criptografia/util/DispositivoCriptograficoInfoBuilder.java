package com.educaflow.subsystem.criptografia.util;

import com.educaflow.base.infrastructure.criptografia.DispositivoCriptografico;
import com.educaflow.base.infrastructure.criptografia.EntornoCriptografico;
import com.educaflow.base.infrastructure.criptografia.impl.helper.CriptografiaUtil;
import com.educaflow.base.infrastructure.criptografia.slot.SlotInfo;
import com.educaflow.base.infrastructure.criptografia.slot.SlotInfoFactory;

import java.nio.file.Path;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.Provider;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Construye el texto informativo (no persistido) de un {@code DispositivoCriptografico}: los
 * datos del {@link SlotInfo} del slot indicado. Todo se lee <strong>en vivo</strong> del
 * dispositivo, de modo que la información es correcta incluso para un dispositivo que aún no se
 * ha guardado ni recargado en el {@link EntornoCriptografico}.
 *
 * <p>Lo invoca el getter calculado del campo {@code info} del dominio, por lo que
 * <strong>nunca lanza excepción</strong>: ante cualquier error devuelve un mensaje que lo
 * describe, para no impedir que se cargue el formulario.
 *
 * <p>También expone {@link #readCertificadosPorAlias}, compartido en el paquete con
 * {@link AliasInfoBuilder} para leer en vivo los certificados del dispositivo por alias.
 */
public final class DispositivoCriptograficoInfoBuilder {

    private DispositivoCriptograficoInfoBuilder() {
    }

    public static String build(String pkcs11LibraryPath, Integer slot, String pin) {
        if (pkcs11LibraryPath == null || pkcs11LibraryPath.isBlank()) {
            return "No se ha indicado la ruta de la librería PKCS#11.";
        }
        int slotIndex = slot != null ? slot : 0;

        try {
            List<SlotInfo> slotsInfo = SlotInfoFactory.getSlotsInfo(Path.of(pkcs11LibraryPath));
            SlotInfo slotInfo = slotsInfo.stream()
                    .filter(s -> s.index == slotIndex)
                    .findFirst()
                    .orElse(null);
            if (slotInfo == null) {
                return "El slot " + slotIndex + " no existe en la librería PKCS#11. "
                        + "Slots disponibles: " + slotsInfo.size() + ".";
            }
            return slotInfo.toString();
        } catch (Exception ex) {
            return "No se pudo leer la información del slot: " + ex.getMessage();
        }
    }

    /**
     * Devuelve los alias (nombres de las entradas del keystore) presentes en el dispositivo,
     * leídos en vivo, en el mismo orden en que los expone el propio keystore.
     */
    public static List<String> listarAlias(String pkcs11LibraryPath, Integer slot, String pin) {
        int slotIndex = slot != null ? slot : 0;
        return new ArrayList<>(readCertificadosPorAlias(pkcs11LibraryPath, slotIndex, pin).keySet());
    }

    /**
     * Devuelve, por cada alias, el certificado del dispositivo. Si el slot ya está configurado
     * en el {@link EntornoCriptografico} reutiliza los datos cacheados (no reabre el token); en
     * caso contrario abre el keystore PKCS#11 directamente con el PIN y elimina después el
     * provider registrado para no dejar estado que interfiera con una posterior recarga del
     * entorno.
     */
    static Map<String, X509Certificate> readCertificadosPorAlias(String pkcs11LibraryPath, int slot, String pin) {
        try {
            DispositivoCriptografico dispositivo = EntornoCriptografico.getDispositivoCriptografico(slot);
            Map<String, X509Certificate> certificados = new LinkedHashMap<>();
            for (String alias : dispositivo.getAliases()) {
                certificados.put(alias, leafCertificate(dispositivo.getCertificateChain(alias)));
            }
            return certificados;
        } catch (Exception ex) {
            // El dispositivo no está (aún) configurado en el entorno: se lee en directo más abajo.
        }

        Provider provider = null;
        try {
            provider = CriptografiaUtil.getProviderPKCS11(Path.of(pkcs11LibraryPath), slot);
            KeyStore keyStore = CriptografiaUtil.getKeyStore(provider, pin, CriptografiaUtil.KeyStoreType.PKCS11);

            Map<String, X509Certificate> certificados = new LinkedHashMap<>();
            Enumeration<String> enumeration = keyStore.aliases();
            while (enumeration.hasMoreElements()) {
                String alias = enumeration.nextElement();
                Certificate certificate = keyStore.getCertificate(alias);
                certificados.put(alias, certificate instanceof X509Certificate ? (X509Certificate) certificate : null);
            }
            return certificados;
        } catch (KeyStoreException ex) {
            throw new RuntimeException("No se pudieron enumerar los alias del keystore PKCS#11", ex);
        } finally {
            if (provider != null) {
                Security.removeProvider(provider.getName());
            }
        }
    }

    private static X509Certificate leafCertificate(Certificate[] chain) {
        if (chain == null || chain.length == 0 || !(chain[0] instanceof X509Certificate)) {
            return null;
        }
        return (X509Certificate) chain[0];
    }
}
