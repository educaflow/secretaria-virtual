package com.educaflow.base.infrastructure.criptografia;

import com.educaflow.base.infrastructure.criptografia.impl.helper.CriptografiaUtil;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 *
 * @author logongas
 */
public class AlmacenClaveFichero implements AlmacenClave {

    private final byte[] contenidoCertificado;
    private final String password;

    public AlmacenClaveFichero(InputStream fileCertificate, String password) {

        if (fileCertificate==null) {
            throw new RuntimeException("El fileCertificate no puede ser null");
        }

        if (password==null) {
            throw new RuntimeException("El password no puede ser null");
        }

        // Se lee el contenido en el constructor porque el almacén hay que leerlo más de una vez:
        // validar el password consume el InputStream y después hay que volver a leerlo para firmar.
        // Además así se cierra el stream de origen, que si no quedaría abierto.
        try (InputStream inputStream = fileCertificate) {
            this.contenidoCertificado = inputStream.readAllBytes();
        } catch (IOException ex) {
            throw new RuntimeException("No se puede leer el fichero del certificado", ex);
        }

        this.password = password;
    }

    /**
     * @return un InputStream nuevo con el contenido del certificado en cada llamada
     */
    public InputStream getFileCertificate() {
        return new ByteArrayInputStream(contenidoCertificado);
    }

    /**
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * Comprueba si el password abre el certificado y permite recuperar su clave privada.
     *
     * @return true si el password es correcto, false si no lo es
     * @throws RuntimeException si el fichero está corrupto o no es un PKCS#12
     */
    public boolean isPasswordValid() {
        try (InputStream inputStream = getFileCertificate()) {
            return CriptografiaUtil.isPasswordValid(inputStream, password);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

}
