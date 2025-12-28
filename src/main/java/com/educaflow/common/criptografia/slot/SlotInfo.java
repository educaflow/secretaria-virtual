package com.educaflow.common.criptografia.slot;

import java.util.Set;

public class SlotInfo {
    public int index;
    public long slotId;
    public String description;
    public String manufacturer;
    public String tokenLabel;
    public String tokenManufacturer;
    public String tokenModel;
    public String tokenSerial;
    public String hardwareVersion;
    public String firmwareVersion;
    public long pinMin;
    public long pinMax;
    public Set<SlotFlag> tokenFlags;

    @Override
    public String toString() {
        return String.format(
                "Slot %d (0x%x): %s\n  Token: %s (%s)\n  Model: %s\n  Serial: %s\n  HW/FW: %s/%s\n  PIN min/max: %d/%d\n  Flags: %s\n",
                index,slotId, description, tokenLabel, tokenManufacturer, tokenModel,
                tokenSerial, hardwareVersion, firmwareVersion,
                pinMin, pinMax, tokenFlags
        );
    }
}