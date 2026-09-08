package com.educaflow.subsystem.criptografia.service;

/**
 * Titular resuelto a partir de un DNI: su nombre, sus apellidos y de dónde salen.
 *
 * <p>{@code tomadoDelUsuario} a {@code true} significa «existe un usuario de la aplicación con ese documento
 * y estos son el nombre y los apellidos de su ficha»; a {@code false} significa «no existe ninguno», y
 * entonces {@code nombre} y {@code apellidos} son {@code null}.
 *
 * <p>Es un value object inmutable, sin identidad y sin persistencia: <strong>MUST NOT</strong> declararse
 * como entidad en ningún {@code domains.xml}. Vive en el paquete {@code service} (no en {@code service.impl})
 * porque forma parte del contrato público: es el tipo de retorno de
 * {@link CertificadoDigitalService#getDatosTitularByDni(String)}.
 *
 * @param nombre           nombre del titular, o {@code null} si no hay usuario con ese DNI
 * @param apellidos        apellidos del titular, o {@code null} si no hay usuario con ese DNI
 * @param tomadoDelUsuario si el nombre y los apellidos se han tomado de la ficha de un usuario de la aplicación
 */
public record DatosTitular(String nombre, String apellidos, boolean tomadoDelUsuario) {

    /**
     * Devuelve el resultado de «no hay ningún usuario de la aplicación con ese DNI»: nombre y apellidos a
     * {@code null} y {@code tomadoDelUsuario} a {@code false}.
     *
     * <p>Existe para que las dos ramas del cálculo se lean simétricas y para que ningún llamante tenga que
     * recordar qué valores lleva esta rama.
     *
     * @return los datos del titular cuando no hay usuario con ese DNI
     */
    public static DatosTitular sinUsuario() {
        return new DatosTitular(null, null, false);
    }
}
