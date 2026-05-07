package com.educaflow.base.infrastructure.criptografia.slot;
import com.sun.jna.*;
import com.sun.jna.ptr.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


public class SlotInfoFactory {

    private static final Logger logger = LoggerFactory.getLogger(SlotInfoFactory.class);
    private static final int CKR_CRYPTOKI_ALREADY_INITIALIZED = 0x191;
    private static final int CKR_USER_ALREADY_LOGGED_IN = 0x100;
    private static final Map<String, PKCS11> libraryCache = new ConcurrentHashMap<>();

    public interface PKCS11 extends Library {
        int C_Initialize(Pointer initArgs);
        int C_GetSlotList(byte tokenPresent, long[] slotList, NativeLongByReference count);
        int C_GetSlotInfo(long slotID, CK_SLOT_INFO info);
        int C_GetTokenInfo(long slotID, CK_TOKEN_INFO info);
        int C_OpenSession(long slotID, long flags, Pointer pApplication, Pointer notify, NativeLongByReference phSession);
        int C_CloseSession(long hSession);
        int C_Login(long hSession, long userType, byte[] pPin, long ulPinLen);
        int C_Logout(long hSession);
    }

    private static final long CKF_SERIAL_SESSION = 0x00000004L;
    private static final long CKU_USER = 1L;

    public static class CK_VERSION extends Structure {
        public byte major;
        public byte minor;
        @Override protected List<String> getFieldOrder() {
            return Arrays.asList("major", "minor");
        }
    }

    // CK_SLOT_INFO
    public static class CK_SLOT_INFO extends Structure {
        public byte[] slotDescription = new byte[64];
        public byte[] manufacturerID = new byte[32];
        public NativeLong flags;
        public CK_VERSION hardwareVersion;
        public CK_VERSION firmwareVersion;
        @Override protected List<String> getFieldOrder() {
            return Arrays.asList("slotDescription","manufacturerID","flags","hardwareVersion","firmwareVersion");
        }
    }

    // CK_TOKEN_INFO
    public static class CK_TOKEN_INFO extends Structure {
        public byte[] label = new byte[32];
        public byte[] manufacturerID = new byte[32];
        public byte[] model = new byte[16];
        public byte[] serialNumber = new byte[16];
        public NativeLong flags;
        public NativeLong ulMaxSessionCount;
        public NativeLong ulSessionCount;
        public NativeLong ulMaxRwSessionCount;
        public NativeLong ulRwSessionCount;
        public NativeLong ulMaxPinLen;
        public NativeLong ulMinPinLen;
        public NativeLong ulTotalPublicMemory;
        public NativeLong ulFreePublicMemory;
        public NativeLong ulTotalPrivateMemory;
        public NativeLong ulFreePrivateMemory;
        public CK_VERSION hardwareVersion;
        public CK_VERSION firmwareVersion;
        public byte[] utcTime = new byte[16];
        @Override protected List<String> getFieldOrder() {
            return Arrays.asList(
                "label","manufacturerID","model","serialNumber","flags",
                "ulMaxSessionCount","ulSessionCount",
                "ulMaxRwSessionCount","ulRwSessionCount",
                "ulMaxPinLen","ulMinPinLen",
                "ulTotalPublicMemory","ulFreePublicMemory",
                "ulTotalPrivateMemory","ulFreePrivateMemory",
                "hardwareVersion","firmwareVersion","utcTime"
            );
        }
    }


    private static String asString(byte[] data) {
        String s = new String(data, StandardCharsets.UTF_8);
        int end = s.length();
        while (end > 0 && (s.charAt(end-1) == '\0' || Character.isWhitespace(s.charAt(end-1)))) end--;
        return s.substring(0, end);
    }

    private static String versionToString(CK_VERSION v) {
        if (v == null) return "0.0";
        int maj = ((int) v.major) & 0xff;
        int min = ((int) v.minor) & 0xff;
        return maj + "." + min;
    }

    /**
     * Esta función hace lo mismo que el comando "pkcs11-tool --list-slots"
     * @param modulePath El path de la librería PKCS#11 (por ejemplo, /usr/lib/x86_64-linux-gnu/opensc-pkcs11.so)
     * @return Lista de slots disponibles en la librería PKCS#11
     */
    public static List<SlotInfo> getSlotsInfo(Path modulePath) {
        logger.info("Obteniendo información de los slots de la librería PKCS#11: {}", modulePath);

        PKCS11 pkcs11 = libraryCache.computeIfAbsent(modulePath.toString(), path -> {
            PKCS11 lib = Native.load(path, PKCS11.class);
            int initRv = lib.C_Initialize(Pointer.NULL);
            if (initRv != 0 && initRv != CKR_CRYPTOKI_ALREADY_INITIALIZED) {
                throw new RuntimeException("C_Initialize error: " + initRv);
            }
            return lib;
        });

        List<SlotInfo> result = new ArrayList<>();
        NativeLongByReference countRef = new NativeLongByReference();

        int rv = pkcs11.C_GetSlotList((byte)0, null, countRef);
        if (rv != 0) throw new RuntimeException("C_GetSlotList (count) error: " + rv);

        int count = countRef.getValue().intValue();
        long[] slots = new long[count];
        rv = pkcs11.C_GetSlotList((byte)0, slots, countRef);
        if (rv != 0) throw new RuntimeException("C_GetSlotList (list) error: " + rv);

        int i = 0;
        for (long sid : slots) {
            CK_SLOT_INFO sinfo = new CK_SLOT_INFO();
            pkcs11.C_GetSlotInfo(sid, sinfo);

            CK_TOKEN_INFO tinfo = new CK_TOKEN_INFO();
            rv = pkcs11.C_GetTokenInfo(sid, tinfo);
            if (rv == 0) {
                SlotInfo slot = new SlotInfo();
                slot.index = i++;
                slot.slotId = sid;
                slot.description = asString(sinfo.slotDescription);
                slot.manufacturer = asString(sinfo.manufacturerID);
                slot.tokenLabel = asString(tinfo.label);
                slot.tokenManufacturer = asString(tinfo.manufacturerID);
                slot.tokenModel = asString(tinfo.model);
                slot.tokenSerial = asString(tinfo.serialNumber);
                slot.hardwareVersion = versionToString(tinfo.hardwareVersion);
                slot.firmwareVersion = versionToString(tinfo.firmwareVersion);
                slot.pinMin = tinfo.ulMinPinLen.longValue();
                slot.pinMax = tinfo.ulMaxPinLen.longValue();
                slot.tokenFlags = SlotFlag.fromMask(tinfo.flags.longValue());
                result.add(slot);
            }
        }

        return result;
    }

    /**
     * Verifica que el PIN es correcto para el slot indicado abriendo una sesión PKCS#11 y llamando a C_Login.
     * Lanza RuntimeException si el PIN es incorrecto o no se puede abrir la sesión.
     * @param modulePath El path de la librería PKCS#11
     * @param slotIndex  El índice del slot (0 para el primero con token, 1 para el segundo…)
     * @param pin        El PIN a verificar
     */
    public static void validatePin(Path modulePath, int slotIndex, String pin) {
        List<SlotInfo> slots = getSlotsInfo(modulePath);
        SlotInfo target = slots.stream()
                .filter(s -> s.index == slotIndex)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("El slot " + slotIndex + " no existe"));

        PKCS11 pkcs11 = libraryCache.get(modulePath.toString());

        NativeLongByReference sessionRef = new NativeLongByReference();
        int rv = pkcs11.C_OpenSession(target.slotId, CKF_SERIAL_SESSION, Pointer.NULL, Pointer.NULL, sessionRef);
        if (rv != 0) throw new RuntimeException("C_OpenSession error: " + rv);

        long hSession = sessionRef.getValue().longValue();
        try {
            byte[] pinBytes = pin != null ? pin.getBytes(StandardCharsets.UTF_8) : new byte[0];
            rv = pkcs11.C_Login(hSession, CKU_USER, pinBytes, pinBytes.length);
            if (rv == CKR_USER_ALREADY_LOGGED_IN) {
                // El dispositivo ya estaba autenticado (p.ej. por EntornoCriptografico al arrancar).
                // Hacemos logout y reintentamos para verificar el PIN proporcionado.
                pkcs11.C_Logout(hSession);
                rv = pkcs11.C_Login(hSession, CKU_USER, pinBytes, pinBytes.length);
            }
            if (rv != 0) throw new RuntimeException("C_Login error: " + rv);
            pkcs11.C_Logout(hSession);
        } finally {
            pkcs11.C_CloseSession(hSession);
        }
    }


}
