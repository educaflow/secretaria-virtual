package com.educaflow.base.infrastructure.fichero;

import java.util.Arrays;
import java.util.Objects;

/**
 * Un fichero que no está guardado en ningún sitio: su nombre, su contenido y su tipo MIME.
 * Es, por ejemplo, el adjunto de un correo o lo que se devuelve en una descarga.
 *
 * <p>Es una clase y no un {@code record} porque el contenido es un {@code byte[]}: el
 * {@code equals}/{@code hashCode} que generaría el record compararía el array por identidad,
 * no por contenido. Aquí la igualdad es por contenido.
 */
public final class Fichero {

    private final String fileName;
    private final byte[] data;
    private final String mimeType;

    public Fichero(String fileName, byte[] data, String mimeType) {
        this.fileName = fileName;
        this.data = data;
        this.mimeType = mimeType;
    }

    public String fileName() {
        return fileName;
    }

    public byte[] data() {
        return data;
    }

    public String mimeType() {
        return mimeType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Fichero that)) {
            return false;
        }
        return Objects.equals(fileName, that.fileName)
                && Arrays.equals(data, that.data)
                && Objects.equals(mimeType, that.mimeType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileName, Arrays.hashCode(data), mimeType);
    }

    @Override
    public String toString() {
        return "Fichero[fileName=" + fileName + ", data=" + (data == null ? "null" : data.length + " bytes") + ", mimeType=" + mimeType + "]";
    }
}
