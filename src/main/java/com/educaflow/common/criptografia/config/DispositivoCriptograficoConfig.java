package com.educaflow.common.criptografia.config;

import java.nio.file.Path;

public class DispositivoCriptograficoConfig {

    private final Path pkcs11LibraryPath;
    private final int slot;
    private final String pin;

    /**
     *
     * @param pkcs11LibraryPath "/usr/lib/x86_64-linux-gnu/opensc-pkcs11.so"
     * @param slot Se averiguan cuantos hay con el comando "pkcs11-tool --list-slots"
     * @param pin El pin del dispositivo
     */
    public DispositivoCriptograficoConfig(Path pkcs11LibraryPath, int slot, String pin) {
        this.pkcs11LibraryPath = pkcs11LibraryPath;
        this.slot = slot;
        this.pin = pin;
    }

    public Path getPkcs11LibraryPath() {
        return pkcs11LibraryPath;
    }

    public int getSlot() {
        return slot;
    }

    public String getPin() {
        return pin;
    }
}
