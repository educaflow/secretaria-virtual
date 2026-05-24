package com.educaflow.subsystem.correos.service;

/**
 * Proporciona la dirección "from" del remitente de los correos que envía la aplicación.
 *
 * <p>El remitente es el usuario SMTP autenticado ({@code mail.smtp.user}, definido en la
 * configuración privada): es la única fuente de verdad y debe coincidir con la cuenta SMTP. Nunca
 * procede del cliente. Esta abstracción aísla a {@code CorreoMailFactory} de la lectura de
 * configuración.
 */
public interface RemitenteProvider {

    /**
     * Devuelve la dirección "from" con la que se envían los correos.
     *
     * @return el remitente; nunca {@code null} ni vacío.
     * @throws IllegalStateException si el remitente no está configurado.
     */
    String getFrom();
}
