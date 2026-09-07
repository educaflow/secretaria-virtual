package com.educaflow.subsystem.criptografia.service;

import com.educaflow.subsystem.criptografia.util.CertificadoDigitalHelper;

/**
 * Situación en la que está una persona para firmar en el servidor.
 *
 * <p>Se deduce ENTERA del certificado digital: DNI válido + qué tipo de almacén guarda el certificado + si
 * su clave está guardada. No menciona ninguna tarea de firma ni ningún expediente, así que su dueño es este
 * subsistema. La calcula {@link CertificadoDigitalHelper#getSituacionFirmaByDni(String)} y la usa cualquiera
 * que quiera firmar en el servidor.
 *
 * <p>Cada situación lleva consigo si con ella {@linkplain #isFirmaEnServidor() corresponde firmar en el
 * servidor}: es un dato de la propia situación y no una clasificación que haga nadie por fuera, así que esa
 * lista de valores <strong>MUST NOT</strong> duplicarse en ningún otro sitio. Al ser un argumento del
 * constructor, un valor nuevo de este enum no compila hasta que quien lo añade decide de qué lado cae, en
 * lugar de degradar en silencio.
 *
 * <p>Es un enum Java normal y <strong>MUST NOT</strong> volver a declararse como enum de dominio en un
 * {@code domains.xml}: no es el tipo de ningún campo de ningún modelo. No se persiste —se recalcula cada vez
 * que hace falta— y a las pantallas viaja como campo de vista que rellena un controlador, comparado como
 * texto en los {@code showIf}, así que tampoco necesita que el cliente reciba títulos suyos.
 */
public enum SituacionFirma {

    /** El firmante no tiene un DNI válido en su ficha, así que no se puede saber qué certificado le corresponde. */
    SIN_DNI(false,false),

    /** El firmante tiene DNI pero no hay ningún certificado digital habilitado dado de alta para él. */
    SIN_CERTIFICADO(false,false),

    /** El certificado del firmante está en un dispositivo criptográfico cuyo PIN ya está guardado. */
    DISPOSITIVO_CON_PIN(true,false),

    /** El certificado del firmante está en un dispositivo criptográfico cuyo PIN no está guardado y hay que pedírselo. */
    DISPOSITIVO_SIN_PIN(true, true),

    /** El certificado del firmante está en un fichero cuya contraseña ya está guardada. */
    FICHERO_CON_CLAVE(true,false),

    /** El certificado del firmante está en un fichero cuya contraseña no está guardada y hay que pedírsela. */
    FICHERO_SIN_CLAVE(true, true);

    private final boolean firmaEnServidor;
    private final boolean necesitaClaveOPin;

    SituacionFirma(boolean firmaEnServidor,boolean necesitaClaveOPin) {
        this.firmaEnServidor = firmaEnServidor;
        this.necesitaClaveOPin = necesitaClaveOPin;
    }

    /**
     * Indica si con esta situación de firma corresponde firmar en el servidor, es decir, si hay un
     * certificado custodiado con el que hacerlo.
     *
     * <p>Es <strong>la</strong> definición de «corresponde firmar en el servidor» del proyecto: la usan tanto
     * quien va a firmar como las reglas de validación y las vistas que deciden qué botón ofrecer.
     *
     * <p>Quien pueda tener la situación a {@code null} <strong>MUST</strong> tolerarlo por su cuenta antes de
     * preguntar, porque este método no se puede invocar sobre una situación ausente. Quien la obtiene de
     * {@link CertificadoDigitalHelper#getSituacionFirmaByDni(String)} no tiene que preocuparse: ese método
     * nunca devuelve {@code null}.
     *
     * @return {@code true} si hay certificado custodiado con el que firmar en el servidor
     */
    public boolean isFirmaEnServidor() {
        return firmaEnServidor;
    }

    public boolean isNecesitaClaveOPin() {
        return necesitaClaveOPin;
    }
}
