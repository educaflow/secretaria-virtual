package com.educaflow.common.criptografia.slot;
import com.sun.jna.*;
import com.sun.jna.ptr.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;


public class SlotInfoFactory {

    public interface PKCS11 extends Library {
        int C_Initialize(Pointer initArgs);
        int C_Finalize(Pointer reserved);
        int C_GetSlotList(byte tokenPresent, long[] slotList, NativeLongByReference count);
        int C_GetSlotInfo(long slotID, CK_SLOT_INFO info);
        int C_GetTokenInfo(long slotID, CK_TOKEN_INFO info);
    }

    // CK_VERSION
    public static class CK_VERSION extends Structure {
        public byte major;
        public byte minor;
        @Override protected List<String> getFieldOrder() {
            return Arrays.asList("major", "minor");
        }
    }

    // CK_DATE (no usado mucho aquí)
    public static class CK_DATE extends Structure {
        public byte[] year = new byte[4];
        public byte[] month = new byte[2];
        public byte[] day = new byte[2];
        @Override protected List<String> getFieldOrder() {
            return Arrays.asList("year","month","day");
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
        public CK_DATE utcTime;
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
     * @return
     */
    public static List<SlotInfo> getSlotsInfo(Path modulePath) {
        PKCS11 pkcs11 = Native.load(modulePath.toString(), PKCS11.class);
        List<SlotInfo> result = new ArrayList<>();

        int rv = pkcs11.C_Initialize(Pointer.NULL);
        if (rv != 0) throw new RuntimeException("C_Initialize error: " + rv);

        try {
            NativeLongByReference countRef = new NativeLongByReference();
            rv = pkcs11.C_GetSlotList((byte)0, null, countRef);
            if (rv != 0) throw new RuntimeException("C_GetSlotList (count) error: " + rv);

            int count = countRef.getValue().intValue();
            long[] slots = new long[count];
            rv = pkcs11.C_GetSlotList((byte)0, slots, countRef);
            if (rv != 0) throw new RuntimeException("C_GetSlotList (list) error: " + rv);

            int i=0;
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
        } finally {
            pkcs11.C_Finalize(Pointer.NULL);
        }

        return result;
    }


}
