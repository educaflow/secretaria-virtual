package com.educaflow.subsystem.correos.service;

import com.axelor.meta.db.MetaFile;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.expedientes.db.HistorialEstado;

import java.util.List;
import java.util.Objects;

/**
 * DTO de alta programática de un {@link com.educaflow.subsystem.correos.db.Correo}.
 *
 * <p>Este DTO <b>es</b> la whitelist de la operación de alta programática (k-secure-coding §3.5):
 * la entrada no pasa por el endpoint REST genérico ni por {@code AllowProperties}, sino que la
 * dicta el subsistema invocador. Por eso puede contener campos {@code servidor} ({@code centro},
 * {@code referenciaHistorial}): es el invocador de confianza quien los aporta (R-Correo-003).
 *
 * <p>Campos obligatorios: {@code asunto}, {@code cuerpo}, {@code dniDestinatario},
 * {@code emailDestinatario} y {@code centro}. {@code referenciaHistorial} y {@code adjuntos}
 * son opcionales.
 */
public record CorreoInsertDTO(
        String asunto,
        String cuerpo,
        String dniDestinatario,
        String emailDestinatario,
        Centro centro,
        HistorialEstado referenciaHistorial,
        List<AdjuntoCorreoInsertDTO> adjuntos) {

    public CorreoInsertDTO {
        Objects.requireNonNull(asunto, "asunto no puede ser null");
        Objects.requireNonNull(cuerpo, "cuerpo no puede ser null");
        Objects.requireNonNull(dniDestinatario, "dniDestinatario no puede ser null");
        Objects.requireNonNull(emailDestinatario, "emailDestinatario no puede ser null");
        Objects.requireNonNull(centro, "centro no puede ser null");
    }

    /**
     * Adjunto de un alta programática de correo. {@code nombreFichero} es el nombre visible y
     * {@code contenido} el {@link MetaFile} con los bytes.
     */
    public record AdjuntoCorreoInsertDTO(String nombreFichero, MetaFile contenido) {

        public AdjuntoCorreoInsertDTO {
            Objects.requireNonNull(nombreFichero, "nombreFichero no puede ser null");
            Objects.requireNonNull(contenido, "contenido no puede ser null");
        }
    }
}
