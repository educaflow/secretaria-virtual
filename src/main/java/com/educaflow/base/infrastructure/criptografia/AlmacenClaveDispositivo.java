package com.educaflow.base.infrastructure.criptografia;

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


    public int getSlot() {
        return slot;
    }

    public String getAlias() {
        return alias;
    }


    
    
    
    
}
