package com.educaflow.base.infrastructure.mail;

import java.util.Arrays;
import java.util.Objects;

/**
 * Adjunto de un correo: nombre de fichero, contenido y tipo MIME.
 *
 * <p>Es una clase y no un {@code record} porque el contenido es un {@code byte[]}: el
 * {@code equals}/{@code hashCode} que generaría el record compararía el array por identidad,
 * no por contenido. Aquí la igualdad es por contenido.
 */
public final class Attach {

    private final String fileName;
    private final byte[] data;
    private final String mimeType;

    public Attach(String fileName, byte[] data, String mimeType) {
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
        if (!(o instanceof Attach that)) {
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
        return "Attach[fileName=" + fileName + ", data=" + (data == null ? "null" : data.length + " bytes") + ", mimeType=" + mimeType + "]";
    }
}
