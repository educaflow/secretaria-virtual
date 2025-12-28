/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.educaflow.common.criptografia;

/**
 *
 * @author logongas
 */
public class AlmacenClaveDispositivo implements AlmacenClave {

    private final int slot;
    private final String alias;

    /**
     *
     * @param slot 0 para el primer dispositivo, 1 para el segundo...Se obtiene con el comando "pkcs11-tool --list-slots"
     * @param alias En el eDNI los valores son "CertAutenticacion" "CertFirmaDigital"
     */
    public AlmacenClaveDispositivo(int slot, String alias) {
        this.slot = slot;
        this.alias = alias;
    }

    /**
     *
     * Se usa el  slot 0
     * @param alias En el eDNI los valores son "CertAutenticacion" "CertFirmaDigital"
     */
    public AlmacenClaveDispositivo(String alias) {
        this.slot = 0;
        this.alias = alias;
    }


    /**
     * @return the slot
     */
    public int getSlot() {
        return slot;
    }

    /**
     * @return the alias
     */
    public String getAlias() {
        return alias;
    }


    
    
    
    
}
